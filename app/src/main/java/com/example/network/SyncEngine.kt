package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.data.model.DeviceMode
import com.example.data.repository.POSRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

data class DiscoveredClient(val name: String, val ip: String, val lastSeen: Long)

class SyncEngine(
    private val context: Context,
    private val repository: POSRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val tag = "SyncEngine"

    private val moshi = Moshi.Builder().build()

    // Connection States
    private val _connectionState = MutableStateFlow<String>("OFFLINE") // OFFLINE, ADVERTISING, DISCOVERING, CONNECTED, SYNCING
    val connectionState: StateFlow<String> = _connectionState

    private val _discoveredDevices = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<NsdServiceInfo>> = _discoveredDevices

    private val _discoveredClients = MutableStateFlow<List<DiscoveredClient>>(emptyList())
    val discoveredClients: StateFlow<List<DiscoveredClient>> = _discoveredClients

    private val _pairedHostIp = MutableStateFlow<String?>(null)
    val pairedHostIp: StateFlow<String?> = _pairedHostIp

    // Server components
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val activeClientSockets = mutableListOf<Socket>()

    // Client components
    private var clientSocket: Socket? = null
    private var clientJob: Job? = null
    private var clientWriter: PrintWriter? = null

    // NSD Discovery
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val serviceType = "_omnipos._tcp"
    private val serviceName = "OmniPOS_${UUID.randomUUID().toString().take(6)}"

    // UDP Broadcast and Listener for robust local network discovery
    private var udpBroadcastJob: Job? = null
    private var udpListenJob: Job? = null

    init {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        startUdpListener() // Listen for active wifi signals continuously
    }

    // --- MAIN DEVICE (SERVER) FUNCTIONS ---

    fun startServer(port: Int = 8080) {
        _connectionState.value = "STARTING_SERVER"
        registerService(port)
        startUdpBroadcast(isServer = true, port = port)
        
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                _connectionState.value = "MASTER_ONLINE"
                Log.d(tag, "Local TCP server started on port $port")
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    Log.d(tag, "Client connected: ${socket.inetAddress.hostAddress}")
                    activeClientSockets.add(socket)
                    handleClientSocket(socket)
                }
            } catch (e: Exception) {
                Log.e(tag, "Server error: ${e.message}")
                _connectionState.value = "SERVER_ERROR"
            }
        }
    }

    private fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = this@SyncEngine.serviceName
            this.serviceType = this@SyncEngine.serviceType
            this.port = port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(tag, "mDNS Service registered: ${info.serviceName}")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "mDNS Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d(tag, "mDNS Service unregistered")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "mDNS Unregistration failed: $errorCode")
            }
        }

        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun handleClientSocket(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                while (socket.isConnected && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    Log.d(tag, "Received from client: $line")
                    val response = processClientMessage(line)
                    writer.println(response)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error handling client connection: ${e.message}")
            } finally {
                activeClientSockets.remove(socket)
                socket.close()
            }
        }
    }

    private suspend fun processClientMessage(message: String): String {
        // Safe robust parsing of requests from Child Device
        return when {
            message.contains("\"type\":\"PING\"") -> {
                "{\"type\":\"PONG\",\"timestamp\":${System.currentTimeMillis()}}"
            }
            message.contains("\"type\":\"SYNC_PUSH\"") -> {
                // Client pushing local transactions queue
                // Extract items
                try {
                    val listStartIndex = message.indexOf("\"queue\":")
                    if (listStartIndex != -1) {
                        val payloadSection = message.substring(listStartIndex + 8)
                        parseAndApplySyncQueue(payloadSection)
                        repository.logAction("SYNC_ENGINE", com.example.data.model.Role.OWNER, "SYNC_PUSH", "Received local sync queue from child client")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to apply synced transactions: ${e.message}")
                }
                "{\"type\":\"SYNC_PUSH_OK\"}"
            }
            message.contains("\"type\":\"SYNC_PULL\"") -> {
                // Return Master Data: Products, Users, Tables, and Orders
                repository.logAction("SYNC_ENGINE", com.example.data.model.Role.OWNER, "SYNC_PULL", "Child requested latest products, tables, and active orders")
                // Write manual response with basic entities
                val productsJson = buildProductsPayload()
                val tablesJson = buildTablesPayload()
                val ordersJson = buildOrdersPayload()
                "{\"type\":\"SYNC_PULL_RESPONSE\",\"products\":$productsJson,\"tables\":$tablesJson,\"orders\":$ordersJson}"
            }
            else -> "{\"type\":\"UNKNOWN_REQUEST\"}"
        }
    }

    private suspend fun parseAndApplySyncQueue(queueJson: String) {
        if (queueJson == "[]" || queueJson.isEmpty()) return
        val items = mutableListOf<String>()
        var start = 0
        var bracesCount = 0
        for (i in queueJson.indices) {
            if (queueJson[i] == '{') {
                if (bracesCount == 0) start = i
                bracesCount++
            }
            if (queueJson[i] == '}') {
                bracesCount--
                if (bracesCount == 0) {
                    items.add(queueJson.substring(start, i + 1))
                }
            }
        }

        items.forEach { payload ->
            val action = extractField(payload, "action") ?: "INSERT/UPDATE"
            val entityType = extractField(payload, "entityType") ?: ""
            val entityId = extractField(payload, "entityId") ?: UUID.randomUUID().toString()
            
            val payloadStart = payload.indexOf("\"payload\":")
            if (payloadStart != -1) {
                var innerPayload = payload.substring(payloadStart + 10).trim()
                if (innerPayload.startsWith("{")) {
                    var count = 0
                    var endIdx = 0
                    for (i in innerPayload.indices) {
                        if (innerPayload[i] == '{') count++
                        if (innerPayload[i] == '}') count--
                        if (count == 0) {
                            endIdx = i + 1
                            break
                        }
                    }
                    innerPayload = innerPayload.substring(0, endIdx)
                } else if (innerPayload.startsWith("\"")) {
                    val secondQuote = innerPayload.indexOf("\"", 1)
                    if (secondQuote != -1) {
                        innerPayload = innerPayload.substring(1, secondQuote)
                    }
                }
                
                if (entityType.isNotEmpty()) {
                    repository.applySyncPayload(entityType, action, entityId, innerPayload)
                }
            }
        }
    }

    private suspend fun buildProductsPayload(): String {
        val list = mutableListOf<String>()
        repository.allProducts.first().forEach { p ->
            list.add("{\"id\":\"${p.id}\",\"name\":\"${p.name}\",\"barcode\":\"${p.barcode}\",\"sku\":\"${p.sku}\",\"category\":\"${p.category}\",\"brand\":\"${p.brand}\",\"costPrice\":${p.costPrice},\"sellingPrice\":${p.sellingPrice},\"stockQuantity\":${p.stockQuantity},\"lowStockAlertLevel\":${p.lowStockAlertLevel},\"isFavorite\":${p.isFavorite},\"batchNumber\":\"${p.batchNumber}\",\"expiryDate\":\"${p.expiryDate}\",\"supplierName\":\"${p.supplierName}\"}")
        }
        return "[${list.joinToString(",")}]"
    }

    private suspend fun buildTablesPayload(): String {
        val list = mutableListOf<String>()
        repository.allTables.first().forEach { t ->
            list.add("{\"id\":\"${t.id}\",\"name\":\"${t.name}\",\"seats\":${t.seats},\"status\":\"${t.status.name}\"}")
        }
        return "[${list.joinToString(",")}]"
    }

    private suspend fun buildOrdersPayload(): String {
        val list = mutableListOf<String>()
        repository.allOrders.first().forEach { o ->
            list.add("{\"id\":\"${o.id}\",\"tableId\":\"${o.tableId ?: ""}\",\"tableName\":\"${o.tableName ?: ""}\",\"type\":\"${o.type.name}\",\"status\":\"${o.status.name}\",\"itemsJson\":${o.itemsJson.let { if(it.isEmpty()) "[]" else it }},\"subtotal\":${o.subtotal},\"discountAmount\":${o.discountAmount},\"taxAmount\":${o.taxAmount},\"totalAmount\":${o.totalAmount},\"paymentMethod\":\"${o.paymentMethod}\",\"waiterName\":\"${o.waiterName}\",\"kitchenNotes\":\"${o.kitchenNotes}\"}")
        }
        return "[${list.joinToString(",")}]"
    }

    // --- CHILD DEVICE (CLIENT) FUNCTIONS ---

    fun startDiscovery() {
        _connectionState.value = "DISCOVERING"
        _discoveredDevices.value = emptyList()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Discovery start failed: $errorCode")
                _connectionState.value = "DISCOVERY_FAILED"
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Discovery stop failed: $errorCode")
            }

            override fun onDiscoveryStarted(regType: String) {
                Log.d(tag, "mDNS Service discovery started")
            }

            override fun onDiscoveryStopped(regType: String) {
                Log.d(tag, "mDNS Service discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(tag, "mDNS Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType.contains(this@SyncEngine.serviceType)) {
                    nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e(tag, "Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                            Log.d(tag, "mDNS Resolved host: ${resolvedInfo.host.hostAddress} port: ${resolvedInfo.port}")
                            val current = _discoveredDevices.value.toMutableList()
                            if (current.none { it.serviceName == resolvedInfo.serviceName }) {
                                current.add(resolvedInfo)
                                _discoveredDevices.value = current
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(tag, "mDNS Service lost: ${serviceInfo.serviceName}")
                val current = _discoveredDevices.value.toMutableList()
                current.removeAll { it.serviceName == serviceInfo.serviceName }
                _discoveredDevices.value = current
            }
        }

        nsdManager?.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun connectToHost(ipAddress: String, port: Int = 8080) {
        _connectionState.value = "CONNECTING"
        _pairedHostIp.value = ipAddress

        clientJob = scope.launch {
            var retries = 0
            while (isActive) {
                try {
                    clientSocket = Socket(ipAddress, port)
                    _connectionState.value = "CONNECTED_TO_MASTER"
                    clientWriter = PrintWriter(clientSocket?.getOutputStream(), true)
                    val reader = BufferedReader(InputStreamReader(clientSocket?.getInputStream()))

                    // Maintain active synchronization loop
                    while (isActive && clientSocket?.isConnected == true) {
                        synchronizeWithMaster()
                        delay(5000) // Sync every 5s
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Connection to master lost: ${e.message}")
                    _connectionState.value = "DISCONNECTED"
                    delay(5000) // Retry every 5s
                    retries++
                    _connectionState.value = "RECONNECTING (Attempt $retries)"
                }
            }
        }
    }

    private suspend fun synchronizeWithMaster() {
        _connectionState.value = "SYNCING_WITH_MASTER"
        try {
            val writer = clientWriter ?: return
            val reader = BufferedReader(InputStreamReader(clientSocket?.getInputStream() ?: return))

            // 1. Push local changes queue first
            val localQueue = repository.getPendingSyncQueue()
            if (localQueue.isNotEmpty()) {
                Log.d(tag, "Pushing ${localQueue.size} items from offline queue to master")
                val queuePayloads = localQueue.map {
                    "{\"action\":\"${it.action}\",\"entityType\":\"${it.entityType}\",\"entityId\":\"${it.entityId}\",\"payload\":${it.payload}}"
                }
                writer.println("{\"type\":\"SYNC_PUSH\",\"queue\":[${queuePayloads.joinToString(",")}]}")
                val pushAck = reader.readLine()
                if (pushAck != null && pushAck.contains("SYNC_PUSH_OK")) {
                    repository.clearSyncQueue()
                    Log.d(tag, "Offline sync queue processed successfully and cleared!")
                }
            }

            // 2. Pull latest product listings, menus, price revisions
            writer.println("{\"type\":\"SYNC_PULL\"}")
            val pullResponse = reader.readLine()
            if (pullResponse != null && pullResponse.contains("SYNC_PULL_RESPONSE")) {
                // Apply latest items in isolated child room DB
                applySyncedMenuData(pullResponse)
            }

            _connectionState.value = "SYNCHRONIZED_ACTIVE"
        } catch (e: Exception) {
            Log.e(tag, "Sync error: ${e.message}")
            _connectionState.value = "CONNECTED_TO_MASTER_SYNC_ERR"
        }
    }

    private suspend fun applySyncedMenuData(response: String) {
        try {
            // Extract products block using manually robust subsegment locator
            val prodStart = response.indexOf("\"products\":[")
            if (prodStart != -1) {
                val rest = response.substring(prodStart + 11)
                var bracketCount = 1
                var endIdx = 0
                for (i in rest.indices) {
                    if (rest[i] == '[') bracketCount++
                    if (rest[i] == ']') bracketCount--
                    if (bracketCount == 0) {
                        endIdx = i + 1
                        break
                    }
                }
                val productsJson = rest.substring(0, endIdx)
                // Parse products and apply locally
                parseAndApplyEntityList("PRODUCT", productsJson)
            }

            // Extract tables block
            val tabStart = response.indexOf("\"tables\":[")
            if (tabStart != -1) {
                val rest = response.substring(tabStart + 9)
                var bracketCount = 1
                var endIdx = 0
                for (i in rest.indices) {
                    if (rest[i] == '[') bracketCount++
                    if (rest[i] == ']') bracketCount--
                    if (bracketCount == 0) {
                        endIdx = i + 1
                        break
                    }
                }
                val tablesJson = rest.substring(0, endIdx)
                parseAndApplyEntityList("TABLE", tablesJson)
            }

            // Extract orders block
            val ordStart = response.indexOf("\"orders\":[")
            if (ordStart != -1) {
                val rest = response.substring(ordStart + 10)
                var bracketCount = 1
                var endIdx = 0
                for (i in rest.indices) {
                    if (rest[i] == '[') bracketCount++
                    if (rest[i] == ']') bracketCount--
                    if (bracketCount == 0) {
                        endIdx = i + 1
                        break
                    }
                }
                val ordersJson = rest.substring(0, endIdx)
                parseAndApplyEntityList("ORDER", ordersJson)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing pulled sync data: ${e.message}")
        }
    }

    private suspend fun parseAndApplyEntityList(type: String, listJson: String) {
        // Perform clean JSON split & local Room insert mapping
        if (listJson == "[]") return
        val items = mutableListOf<String>()
        var start = 0
        var bracesCount = 0
        for (i in listJson.indices) {
            if (listJson[i] == '{') {
                if (bracesCount == 0) start = i
                bracesCount++
            }
            if (listJson[i] == '}') {
                bracesCount--
                if (bracesCount == 0) {
                    items.add(listJson.substring(start, i + 1))
                }
            }
        }

        items.forEach { payload ->
            val entityId = extractField(payload, "id") ?: UUID.randomUUID().toString()
            repository.applySyncPayload(type, "INSERT/UPDATE", entityId, payload)
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting local IP: ${e.message}")
        }
        return "127.0.0.1"
    }

    fun startUdpBroadcast(isServer: Boolean, port: Int = 8080) {
        udpBroadcastJob?.cancel()
        udpBroadcastJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                }
                while (isActive) {
                    val localIp = getLocalIpAddress()
                    val payload = if (isServer) {
                        "{\"type\":\"OMNIPOS_SERVER\",\"name\":\"$serviceName\",\"ip\":\"$localIp\",\"port\":$port}"
                    } else {
                        "{\"type\":\"OMNIPOS_CLIENT\",\"name\":\"$serviceName\",\"ip\":\"$localIp\"}"
                    }
                    val bytes = payload.toByteArray()
                    val packet = DatagramPacket(
                        bytes,
                        bytes.size,
                        InetAddress.getByName("255.255.255.255"),
                        8888
                    )
                    socket.send(packet)
                    delay(2500)
                }
            } catch (e: Exception) {
                Log.e(tag, "UDP Broadcast error: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    fun startUdpListener() {
        udpListenJob?.cancel()
        udpListenJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(8888).apply {
                    reuseAddress = true
                }
                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    val senderIp = packet.address.hostAddress ?: ""
                    
                    // Skip our own broadcasts
                    if (senderIp == getLocalIpAddress()) continue
                    
                    if (message.contains("OMNIPOS_SERVER")) {
                        val name = extractField(message, "name") ?: "OmniPOS Server"
                        val ip = extractField(message, "ip") ?: senderIp
                        val portStr = extractField(message, "port") ?: "8080"
                        val portNum = portStr.toIntOrNull() ?: 8080
                        
                        val serviceInfo = NsdServiceInfo().apply {
                            this.serviceName = name
                            this.port = portNum
                            this.host = InetAddress.getByName(ip)
                        }
                        
                        val current = _discoveredDevices.value.toMutableList()
                        if (current.none { it.serviceName == name || (it.host != null && it.host.hostAddress == ip) }) {
                            current.add(serviceInfo)
                            _discoveredDevices.value = current
                        }
                    } else if (message.contains("OMNIPOS_CLIENT")) {
                        val name = extractField(message, "name") ?: "Child Terminal"
                        val ip = extractField(message, "ip") ?: senderIp
                        
                        val client = DiscoveredClient(name, ip, System.currentTimeMillis())
                        val current = _discoveredClients.value.toMutableList()
                        current.removeAll { it.ip == ip || it.name == name }
                        current.add(client)
                        _discoveredClients.value = current
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "UDP Listener error: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    private fun extractField(json: String, field: String): String? {
        val pattern = "\"$field\"\\s*:\\s*\"?([^\",}]*)\"?".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.trim()
    }

    // --- TEARDOWN ---

    fun shutdown() {
        try {
            // Unregister NSD service
            if (registrationListener != null) {
                nsdManager?.unregisterService(registrationListener)
            }
            // Stop service discovery
            if (discoveryListener != null) {
                nsdManager?.stopServiceDiscovery(discoveryListener)
            }
        } catch (e: Exception) {
            Log.e(tag, "Teardown NSD error: ${e.message}")
        }

        udpBroadcastJob?.cancel()
        udpListenJob?.cancel()
        serverJob?.cancel()
        clientJob?.cancel()

        activeClientSockets.forEach {
            runCatching { it.close() }
        }
        activeClientSockets.clear()

        runCatching { serverSocket?.close() }
        runCatching { clientSocket?.close() }

        _connectionState.value = "OFFLINE"
    }
}
