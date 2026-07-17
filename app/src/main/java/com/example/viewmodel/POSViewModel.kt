package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.POSRepository
import com.example.network.SyncEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class POSViewModel(
    private val repository: POSRepository,
    private val context: Context
) : ViewModel() {

    val syncEngine = SyncEngine(context, repository)

    // UI Navigation State
    private val _currentScreen = MutableStateFlow("SETUP") // SETUP, LOGIN, DASHBOARD
    val currentScreen: StateFlow<String> = _currentScreen

    private val _subScreen = MutableStateFlow("RETAIL") // RETAIL, RESTAURANT, KITCHEN_DISPLAY, INVENTORY, REPORTS, PRINTER, SETTINGS
    val subScreen: StateFlow<String> = _subScreen

    // Auth & Device Roles
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _deviceMode = MutableStateFlow<DeviceMode?>(null)
    val deviceMode: StateFlow<DeviceMode?> = _deviceMode

    // Domain States
    val products = repository.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tables = repository.allTables.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val orders = repository.allOrders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val openShift = repository.currentOpenShift.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val shifts = repository.allShifts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val users = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val syncQueue = repository.syncQueue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = repository.auditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val suppliers = repository.allSuppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val promotions = repository.allPromotions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Network Sync States
    val syncState = syncEngine.connectionState
    val discoveredHosts = syncEngine.discoveredDevices
    val discoveredClients = syncEngine.discoveredClients
    val pairedIp = syncEngine.pairedHostIp

    // Retail Cart State
    private val _cart = MutableStateFlow<List<OrderItem>>(emptyList())
    val cart: StateFlow<List<OrderItem>> = _cart

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Table view selections
    private val _selectedTable = MutableStateFlow<Table?>(null)
    val selectedTable: StateFlow<Table?> = _selectedTable

    // Thermal Printer state simulator
    private val _receiptPreview = MutableStateFlow<String>("")
    val receiptPreview: StateFlow<String> = _receiptPreview

    init {
        // Seed default users if empty
        viewModelScope.launch {
            users.collect { list ->
                if (list.isEmpty()) {
                    repository.insertUser(User("1", "Admin Owner", "1111", Role.OWNER, "[\"ALL\"]"), false)
                    repository.insertUser(User("2", "John Cashier", "2222", Role.CASHIER, "[\"RETAIL\"]"), false)
                    repository.insertUser(User("3", "Chef Roberto", "3333", Role.KITCHEN, "[\"KITCHEN\"]"), false)
                    repository.insertUser(User("4", "Maria Waiter", "4444", Role.WAITER, "[\"RESTAURANT\"]"), false)
                    
                    // Seed initial products
                    repository.insertProduct(Product("p1", "Organic Espresso Beans", "8801", "SKU-ESP-01", "Coffee Beans", "Aroma Co", 8.50, 18.00, 150.0, 10.0, true), false)
                    repository.insertProduct(Product("p2", "Cold Brew Bottle 500ml", "8802", "SKU-CB-02", "Cold Beverages", "Aroma Co", 2.10, 5.50, 85.0, 15.0, true), false)
                    repository.insertProduct(Product("p3", "Croissant (Butter)", "8803", "SKU-BAK-03", "Bakery", "Gourmet Bakery", 0.90, 3.20, 45.0, 5.0, false), false)
                    repository.insertProduct(Product("p4", "Matcha Latte Mix", "8804", "SKU-MAT-04", "Teas", "Kyoto Farm", 12.00, 24.50, 20.0, 3.0, false), false)
                    repository.insertProduct(Product("p5", "Avocado Sourdough Toast", "8805", "SKU-FOOD-05", "Hot Dishes", "Kitchen", 3.50, 11.50, 60.0, 8.0, true), false)
                    repository.insertProduct(Product("p6", "Fiji Mineral Water", "8806", "SKU-CB-06", "Cold Beverages", "Fiji", 0.50, 2.00, 200.0, 20.0, false), false)

                    // Seed initial tables
                    repository.insertTable(Table("t1", "Table 01 (Indoor)", 2, TableStatus.AVAILABLE), false)
                    repository.insertTable(Table("t2", "Table 02 (Indoor)", 4, TableStatus.AVAILABLE), false)
                    repository.insertTable(Table("t3", "Table 03 (Window)", 2, TableStatus.AVAILABLE), false)
                    repository.insertTable(Table("t4", "Table 04 (Window)", 4, TableStatus.AVAILABLE), false)
                    repository.insertTable(Table("t5", "Patio Desk A", 2, TableStatus.AVAILABLE), false)
                    repository.insertTable(Table("t6", "Patio Desk B", 6, TableStatus.AVAILABLE), false)
                }
            }
        }
    }

    // --- NAVIGATION ---
    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun setSubScreen(subScreen: String) {
        _subScreen.value = subScreen
    }

    // --- DEVICE MODE CONFIG ---
    fun selectDeviceMode(mode: DeviceMode) {
        _deviceMode.value = mode
        if (mode == DeviceMode.SERVER_MAIN) {
            syncEngine.startServer()
        } else {
            syncEngine.startDiscovery()
        }
        _currentScreen.value = "LOGIN"
    }

    // --- AUTHENTICATION ---
    fun loginWithPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = repository.authenticateUser(pin)
            if (user != null) {
                _currentUser.value = user
                repository.logAction(user.name, user.role, "LOGIN", "Successfully unlocked terminal PIN")
                _currentScreen.value = "DASHBOARD"
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null) {
                repository.logAction(user.name, user.role, "LOGOUT", "Locked active POS session")
            }
            _currentUser.value = null
            _currentScreen.value = "LOGIN"
        }
    }

    // --- CART AND CHECKOUT ---
    fun addToCart(product: Product, variant: String = "", modifiers: List<String> = emptyList()) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.productId == product.id && it.selectedVariant == variant }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1)
        } else {
            current.add(OrderItem(product.id, product.name, 1, product.sellingPrice, variant, modifiers))
        }
        _cart.value = current
    }

    fun updateCartItemQty(index: Int, newQty: Int) {
        if (newQty <= 0) {
            _cart.value = _cart.value.filterIndexed { i, _ -> i != index }
        } else {
            _cart.value = _cart.value.mapIndexed { i, item ->
                if (i == index) item.copy(quantity = newQty) else item
            }
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- ORDER CHECKOUT (RETAIL & RESTAURANT) ---
    fun checkout(paymentMethod: String, discountAmount: Double = 0.0, waiterName: String = "", notes: String = "") {
        val user = _currentUser.value ?: return
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return

        val subtotal = currentCart.sumOf { it.quantity * it.unitPrice }
        val tax = (subtotal - discountAmount) * 0.10 // 10% Flat Tax
        val total = (subtotal - discountAmount) + tax

        viewModelScope.launch {
            // Build Serialized items list
            val itemsString = currentCart.map {
                "{\"productId\":\"${it.productId}\",\"name\":\"${it.name}\",\"quantity\":${it.quantity},\"unitPrice\":${it.unitPrice},\"selectedVariant\":\"${it.selectedVariant}\"}"
            }.joinToString(",")
            val itemsJson = "[$itemsString]"

            val orderId = UUID.randomUUID().toString()
            val order = Order(
                id = orderId,
                tableId = _selectedTable.value?.id,
                tableName = _selectedTable.value?.name,
                orderNumber = (1000..9999).random(),
                type = if (_selectedTable.value != null) OrderType.DINE_IN else OrderType.TAKE_AWAY,
                status = if (_selectedTable.value != null) OrderStatus.PREPARING else OrderStatus.COMPLETED,
                itemsJson = itemsJson,
                subtotal = subtotal,
                discountAmount = discountAmount,
                taxAmount = tax,
                totalAmount = total,
                paymentMethod = paymentMethod,
                waiterName = waiterName.ifEmpty { user.name },
                kitchenNotes = notes,
                isSynced = false,
                timestamp = System.currentTimeMillis()
            )

            // Insert into local DB & reduce catalog stock
            repository.insertOrder(order)
            currentCart.forEach { item ->
                val p = products.value.find { it.id == item.productId }
                if (p != null) {
                    repository.updateProductStock(p.id, (p.stockQuantity - item.quantity).coerceAtLeast(0.0))
                }
            }

            // If Dine-In, update table status to occupied
            _selectedTable.value?.let { t ->
                repository.updateTableStatus(t.id, TableStatus.OCCUPIED)
            }

            repository.logAction(user.name, user.role, "CHECKOUT", "Generated Order #:${order.orderNumber} Total: $$total")
            
            // Build simulated high-fidelity ESC/POS Receipt layout
            generateReceiptPreview(order, currentCart)

            // Reset UI States
            _cart.value = emptyList()
            _selectedTable.value = null
            _subScreen.value = "PRINTER"
        }
    }

    private fun generateReceiptPreview(order: Order, items: List<OrderItem>) {
        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append("               OMNIPOS CO.              \n")
        sb.append("         Enterprise Retail Center       \n")
        sb.append("         Local Offline IP Server        \n")
        sb.append("========================================\n")
        sb.append("Order Ref : ${order.id.take(8).uppercase()}\n")
        sb.append("Ticket No : #${order.orderNumber}\n")
        sb.append("Cashier   : ${order.waiterName}\n")
        sb.append("Mode      : ${order.type.name}\n")
        sb.append("Date      : ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(order.timestamp)}\n")
        sb.append("----------------------------------------\n")
        items.forEach {
            sb.append(String.format("%-22s %2d x %5.2f\n", it.name.take(22), it.quantity, it.unitPrice))
            if (it.selectedVariant.isNotEmpty()) {
                sb.append("  ↳ Var: ${it.selectedVariant}\n")
            }
        }
        sb.append("----------------------------------------\n")
        sb.append(String.format("Subtotal:                        $%6.2f\n", order.subtotal))
        sb.append(String.format("Discount:                       -$%6.2f\n", order.discountAmount))
        sb.append(String.format("Sales Tax (10%%):                 $%6.2f\n", order.taxAmount))
        sb.append("----------------------------------------\n")
        sb.append(String.format("TOTAL BILL:                      $%6.2f\n", order.totalAmount))
        sb.append("========================================\n")
        sb.append("Payment Mode : [ ${order.paymentMethod} ]\n")
        sb.append("     *** THANK YOU FOR YOUR VISIT ***    \n")
        sb.append("        ESC/POS Thermal Code QR 2D      \n")
        sb.append("========================================\n")
        _receiptPreview.value = sb.toString()
    }

    // --- DINE IN TABLE ACTIONS ---
    fun selectTable(table: Table?) {
        _selectedTable.value = table
    }

    fun completeTableBill(table: Table) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateTableStatus(table.id, TableStatus.AVAILABLE)
            // find active preparing orders of this table and mark completed
            val activeOrder = orders.value.find { it.tableId == table.id && it.status != OrderStatus.COMPLETED }
            if (activeOrder != null) {
                repository.updateOrderStatus(activeOrder.id, OrderStatus.COMPLETED)
            }
            repository.logAction(user.name, user.role, "TABLE_SETTLE", "Settled Table ${table.name}")
            _selectedTable.value = null
        }
    }

    // --- KITCHEN DISPLAY ACTION ---
    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            repository.logAction(user.name, user.role, "KITCHEN_FLOW", "Updated order ${orderId.take(6)} to status: $newStatus")
        }
    }

    // --- SHIFTS DRAWER MANAGEMENT ---
    fun openNewShift(openingCash: Double) {
        val user = _currentUser.value ?: return
        val shift = Shift(
            id = UUID.randomUUID().toString(),
            cashierName = user.name,
            openTime = System.currentTimeMillis(),
            openingCash = openingCash,
            status = "OPEN"
        )
        viewModelScope.launch {
            repository.openShift(shift)
            repository.logAction(user.name, user.role, "SHIFT_OPEN", "Opened new checkout drawer with: $$openingCash")
        }
    }

    fun closeActiveShift(closingCash: Double) {
        val user = _currentUser.value ?: return
        val active = openShift.value ?: return
        viewModelScope.launch {
            repository.closeShift(active.id, closingCash)
            repository.logAction(user.name, user.role, "SHIFT_CLOSE", "Closed drawer. Expected: $${active.openingCash} Actual: $$closingCash")
        }
    }

    // --- INVENTORY ADJUSTMENT ---
    fun addNewProduct(name: String, barcode: String, sku: String, category: String, cost: Double, price: Double, stock: Double) {
        viewModelScope.launch {
            val item = Product(
                id = UUID.randomUUID().toString(),
                name = name,
                barcode = barcode,
                sku = sku,
                category = category,
                brand = "OmniPOS Store",
                costPrice = cost,
                sellingPrice = price,
                stockQuantity = stock,
                lowStockAlertLevel = 5.0
            )
            repository.insertProduct(item)
            val user = _currentUser.value
            if (user != null) {
                repository.logAction(user.name, user.role, "INVENTORY_ADD", "Added product $name to stock listings")
            }
        }
    }

    // --- NETWORK PAIRING FOR CLIENTS ---
    fun pairWithServerHost(ipAddress: String) {
        syncEngine.connectToHost(ipAddress)
    }

    fun startBroadcastingPresence() {
        syncEngine.startUdpBroadcast(isServer = false)
    }

    override fun onCleared() {
        syncEngine.shutdown()
        super.onCleared()
    }
}
