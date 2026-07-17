package com.example.data.repository

import com.example.data.db.POSDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class POSRepository(private val db: POSDatabase) {

    // DAOs
    private val productDao = db.productDao()
    private val tableDao = db.tableDao()
    private val orderDao = db.orderDao()
    private val shiftDao = db.shiftDao()
    private val userDao = db.userDao()
    private val syncQueueDao = db.syncQueueDao()
    private val auditDao = db.auditDao()
    private val supplierDao = db.supplierDao()
    private val promotionDao = db.promotionDao()

    // Flow streams
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allTables: Flow<List<Table>> = tableDao.getAllTables()
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    val currentOpenShift: Flow<Shift?> = shiftDao.getOpenShift()
    val allShifts: Flow<List<Shift>> = shiftDao.getAllShifts()
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val syncQueue: Flow<List<SyncQueue>> = syncQueueDao.getQueueFlow()
    val auditLogs: Flow<List<AuditLog>> = auditDao.getLogs()
    val allSuppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    val allPromotions: Flow<List<Promotion>> = promotionDao.getAllPromotions()

    // Auditing helper
    suspend fun logAction(username: String, role: Role, action: String, details: String) {
        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            username = username,
            role = role.name,
            action = action,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        auditDao.insertLog(log)
    }

    // Products
    suspend fun insertProduct(product: Product, enqueue: Boolean = true) {
        productDao.insertProduct(product)
        if (enqueue) {
            enqueueSync("PRODUCT", product.id, "INSERT/UPDATE", "{\"id\":\"${product.id}\",\"name\":\"${product.name}\",\"barcode\":\"${product.barcode}\",\"sku\":\"${product.sku}\",\"category\":\"${product.category}\",\"brand\":\"${product.brand}\",\"costPrice\":${product.costPrice},\"sellingPrice\":${product.sellingPrice},\"stockQuantity\":${product.stockQuantity},\"lowStockAlertLevel\":${product.lowStockAlertLevel},\"isFavorite\":${product.isFavorite},\"batchNumber\":\"${product.batchNumber}\",\"expiryDate\":\"${product.expiryDate}\",\"supplierName\":\"${product.supplierName}\"}")
        }
    }

    suspend fun updateProductStock(id: String, quantity: Double) {
        productDao.updateStock(id, quantity)
    }

    suspend fun deleteProduct(id: String, enqueue: Boolean = true) {
        productDao.deleteProduct(id)
        if (enqueue) {
            enqueueSync("PRODUCT", id, "DELETE", "{}")
        }
    }

    suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode)
    }

    // Tables
    suspend fun insertTable(table: Table, enqueue: Boolean = true) {
        tableDao.insertTable(table)
        if (enqueue) {
            enqueueSync("TABLE", table.id, "INSERT/UPDATE", "{\"id\":\"${table.id}\",\"name\":\"${table.name}\",\"seats\":${table.seats},\"status\":\"${table.status.name}\"}")
        }
    }

    suspend fun updateTableStatus(id: String, status: TableStatus, enqueue: Boolean = true) {
        tableDao.updateTableStatus(id, status)
        if (enqueue) {
            enqueueSync("TABLE", id, "UPDATE_STATUS", "{\"status\":\"${status.name}\"}")
        }
    }

    // Orders & Sales
    suspend fun insertOrder(order: Order, enqueue: Boolean = true) {
        orderDao.insertOrder(order)
        if (enqueue) {
            enqueueSync("ORDER", order.id, "INSERT/UPDATE", "{\"id\":\"${order.id}\",\"tableId\":\"${order.tableId ?: ""}\",\"tableName\":\"${order.tableName ?: ""}\",\"type\":\"${order.type.name}\",\"status\":\"${order.status.name}\",\"itemsJson\":${order.itemsJson.let { if(it.isEmpty()) "[]" else it }},\"subtotal\":${order.subtotal},\"discountAmount\":${order.discountAmount},\"taxAmount\":${order.taxAmount},\"totalAmount\":${order.totalAmount},\"paymentMethod\":\"${order.paymentMethod}\",\"waiterName\":\"${order.waiterName}\",\"kitchenNotes\":\"${order.kitchenNotes}\"}")
        }
    }

    suspend fun updateOrderStatus(id: String, status: OrderStatus, enqueue: Boolean = true) {
        orderDao.updateOrderStatus(id, status)
        if (enqueue) {
            enqueueSync("ORDER", id, "UPDATE_STATUS", "{\"status\":\"${status.name}\"}")
        }
    }

    suspend fun markOrderAsSynced(id: String) {
        orderDao.markAsSynced(id)
    }

    suspend fun getUnsyncedOrders(): List<Order> {
        return orderDao.getUnsyncedOrders()
    }

    // Shifts
    suspend fun openShift(shift: Shift) {
        shiftDao.insertShift(shift)
    }

    suspend fun closeShift(id: String, closingCash: Double) {
        shiftDao.closeShift(id, System.currentTimeMillis(), closingCash)
    }

    // Users
    suspend fun authenticateUser(pin: String): User? {
        return userDao.getUserByPin(pin)
    }

    suspend fun insertUser(user: User, enqueue: Boolean = true) {
        userDao.insertUser(user)
        if (enqueue) {
            enqueueSync("USER", user.id, "INSERT/UPDATE", "{\"id\":\"${user.id}\",\"name\":\"${user.name}\",\"pin\":\"${user.pin}\",\"role\":\"${user.role.name}\",\"permissionsJson\":\"${user.permissionsJson}\"}")
        }
    }

    suspend fun deleteUser(id: String, enqueue: Boolean = true) {
        userDao.deleteUser(id)
        if (enqueue) {
            enqueueSync("USER", id, "DELETE", "{}")
        }
    }

    // Suppliers
    suspend fun insertSupplier(supplier: Supplier) {
        supplierDao.insertSupplier(supplier)
    }

    // Promotions
    suspend fun insertPromotion(promotion: Promotion) {
        promotionDao.insertPromotion(promotion)
    }

    // Sync Queue Management
    suspend fun enqueueSync(entityType: String, entityId: String, action: String, payload: String) {
        val item = SyncQueue(
            action = action,
            entityType = entityType,
            entityId = entityId,
            payload = payload,
            timestamp = System.currentTimeMillis()
        )
        syncQueueDao.insertQueue(item)
    }

    suspend fun getPendingSyncQueue(): List<SyncQueue> {
        return syncQueueDao.getQueue()
    }

    suspend fun deleteSyncQueueItem(id: Long) {
        syncQueueDao.deleteQueueItem(id)
    }

    suspend fun clearSyncQueue() {
        syncQueueDao.clearQueue()
    }

    // Master Bulk Database Insert/Replacements (called on sync completion)
    suspend fun applySyncPayload(type: String, action: String, entityId: String, payload: String) {
        // Parse basic payload strings to insert Room entities directly
        // This is safe, offline-first conflict-resolution for peer clients
        when (type) {
            "PRODUCT" -> {
                if (action == "DELETE") {
                    productDao.deleteProduct(entityId)
                } else {
                    // Extract fields manually from simple JSON payload to avoid heavy reflection crashes
                    val name = extractField(payload, "name") ?: "Product"
                    val barcode = extractField(payload, "barcode") ?: ""
                    val sku = extractField(payload, "sku") ?: ""
                    val category = extractField(payload, "category") ?: "General"
                    val brand = extractField(payload, "brand") ?: ""
                    val costPrice = extractFieldDouble(payload, "costPrice")
                    val sellingPrice = extractFieldDouble(payload, "sellingPrice")
                    val stockQuantity = extractFieldDouble(payload, "stockQuantity")
                    val lowStock = extractFieldDouble(payload, "lowStockAlertLevel")
                    val isFavorite = payload.contains("\"isFavorite\":true")
                    val batchNumber = extractField(payload, "batchNumber") ?: ""
                    val expiryDate = extractField(payload, "expiryDate") ?: ""
                    val supplierName = extractField(payload, "supplierName") ?: ""

                    val product = Product(
                        id = entityId,
                        name = name,
                        barcode = barcode,
                        sku = sku,
                        category = category,
                        brand = brand,
                        costPrice = costPrice,
                        sellingPrice = sellingPrice,
                        stockQuantity = stockQuantity,
                        lowStockAlertLevel = lowStock,
                        isFavorite = isFavorite,
                        batchNumber = batchNumber,
                        expiryDate = expiryDate,
                        supplierName = supplierName
                    )
                    productDao.insertProduct(product)
                }
            }
            "ORDER" -> {
                if (action == "DELETE") {
                    orderDao.deleteOrder(entityId)
                } else if (action == "UPDATE_STATUS") {
                    val statusStr = extractField(payload, "status") ?: "PENDING"
                    orderDao.updateOrderStatus(entityId, OrderStatus.valueOf(statusStr))
                } else {
                    val tableId = extractField(payload, "tableId")
                    val tableName = extractField(payload, "tableName")
                    val typeName = extractField(payload, "type") ?: OrderType.DINE_IN.name
                    val statusName = extractField(payload, "status") ?: OrderStatus.PENDING.name
                    val subtotal = extractFieldDouble(payload, "subtotal")
                    val discount = extractFieldDouble(payload, "discountAmount")
                    val tax = extractFieldDouble(payload, "taxAmount")
                    val total = extractFieldDouble(payload, "totalAmount")
                    val payMethod = extractField(payload, "paymentMethod") ?: "CASH"
                    val waiter = extractField(payload, "waiterName") ?: ""
                    val notes = extractField(payload, "kitchenNotes") ?: ""
                    
                    // Simple regex extract JSON list of items
                    val itemsStartIndex = payload.indexOf("\"itemsJson\":")
                    val itemsJson = if (itemsStartIndex != -1) {
                        val rest = payload.substring(itemsStartIndex + 12)
                        if (rest.startsWith("[")) {
                            var bracketCount = 0
                            var endIdx = 0
                            for (i in rest.indices) {
                                if (rest[i] == '[') bracketCount++
                                if (rest[i] == ']') bracketCount--
                                if (bracketCount == 0) {
                                    endIdx = i + 1
                                    break
                                }
                            }
                            rest.substring(0, endIdx)
                        } else "[]"
                    } else "[]"

                    val order = Order(
                        id = entityId,
                        tableId = tableId,
                        tableName = tableName,
                        type = OrderType.valueOf(typeName),
                        status = OrderStatus.valueOf(statusName),
                        itemsJson = itemsJson,
                        subtotal = subtotal,
                        discountAmount = discount,
                        taxAmount = tax,
                        totalAmount = total,
                        paymentMethod = payMethod,
                        waiterName = waiter,
                        kitchenNotes = notes,
                        isSynced = true,
                        timestamp = System.currentTimeMillis()
                    )
                    orderDao.insertOrder(order)
                }
            }
            "TABLE" -> {
                if (action == "UPDATE_STATUS") {
                    val status = extractField(payload, "status")?.let { TableStatus.valueOf(it) } ?: TableStatus.AVAILABLE
                    tableDao.updateTableStatus(entityId, status)
                } else {
                    val name = extractField(payload, "name") ?: "Table"
                    val seats = extractFieldInt(payload, "seats")
                    val statusStr = extractField(payload, "status") ?: "AVAILABLE"
                    val table = Table(
                        id = entityId,
                        name = name,
                        seats = seats,
                        status = TableStatus.valueOf(statusStr)
                    )
                    tableDao.insertTable(table)
                }
            }
            "USER" -> {
                if (action == "DELETE") {
                    userDao.deleteUser(entityId)
                } else {
                    val name = extractField(payload, "name") ?: "User"
                    val pin = extractField(payload, "pin") ?: "1234"
                    val roleStr = extractField(payload, "role") ?: "CASHIER"
                    val perms = extractField(payload, "permissionsJson") ?: "[]"
                    val user = User(
                        id = entityId,
                        name = name,
                        pin = pin,
                        role = Role.valueOf(roleStr),
                        permissionsJson = perms
                    )
                    userDao.insertUser(user)
                }
            }
        }
    }

    // Helper utilities for manual JSON parsing (avoids Moshi crashes on custom formats)
    private fun extractField(json: String, field: String): String? {
        val pattern = "\"$field\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractFieldDouble(json: String, field: String): Double {
        val pattern = "\"$field\"\\s*:\\s*([0-9.]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun extractFieldInt(json: String, field: String): Int {
        val pattern = "\"$field\"\\s*:\\s*([0-9]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
}
