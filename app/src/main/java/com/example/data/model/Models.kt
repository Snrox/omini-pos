package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Role {
    OWNER, ADMINISTRATOR, CASHIER, MANAGER, KITCHEN, WAITER, INVENTORY_MANAGER, ACCOUNTANT
}

enum class DeviceMode {
    SERVER_MAIN, CLIENT_CHILD
}

enum class OrderType {
    DINE_IN, TAKE_AWAY, DELIVERY
}

enum class OrderStatus {
    PENDING, PREPARING, READY, SERVED, COMPLETED, CANCELLED
}

enum class TableStatus {
    AVAILABLE, OCCUPIED, RESERVED, DIRTY
}

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val name: String,
    val barcode: String,
    val sku: String,
    val category: String,
    val brand: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val stockQuantity: Double,
    val lowStockAlertLevel: Double,
    val isFavorite: Boolean = false,
    val hasVariants: Boolean = false,
    val variantsJson: String = "[]", // List of variant details
    val modifiersJson: String = "[]", // List of customizable options
    val batchNumber: String = "",
    val expiryDate: String = "",
    val supplierName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tables")
data class Table(
    @PrimaryKey val id: String,
    val name: String,
    val seats: Int,
    val status: TableStatus = TableStatus.AVAILABLE,
    val floorX: Float = 0f,
    val floorY: Float = 0f
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val id: String,
    val tableId: String? = null,
    val tableName: String? = null,
    val orderNumber: Int = 0,
    val type: OrderType = OrderType.DINE_IN,
    val status: OrderStatus = OrderStatus.PENDING,
    val itemsJson: String, // List of OrderItem
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val paymentMethod: String = "", // CASH, CARD, SPLIT, QR_PAYMENT, GIFT_CARD, STORE_CREDIT
    val paymentDetailsJson: String = "{}",
    val waiterName: String = "",
    val kitchenNotes: String = "",
    val isSynced: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class OrderItem(
    val productId: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val selectedVariant: String = "",
    val selectedModifiers: List<String> = emptyList(),
    val note: String = ""
)

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey val id: String,
    val cashierName: String,
    val openTime: Long,
    val closeTime: Long? = null,
    val openingCash: Double,
    val closingCash: Double? = null,
    val cashIn: Double = 0.0,
    val cashOut: Double = 0.0,
    val transactionsJson: String = "[]", // Summary of transactions
    val status: String = "OPEN" // OPEN, CLOSED
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val pin: String,
    val role: Role,
    val permissionsJson: String // Set of allowed screens/actions
)

@Entity(tableName = "sync_queue")
data class SyncQueue(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String, // INSERT, UPDATE, DELETE
    val entityType: String, // PRODUCT, ORDER, TABLE, USER, SHIFT
    val entityId: String,
    val payload: String, // JSON content
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey val id: String,
    val username: String,
    val role: String,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey val id: String,
    val name: String,
    val contactPerson: String,
    val phone: String,
    val outstandingBalance: Double = 0.0
)

@Entity(tableName = "promotions")
data class Promotion(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val type: String, // PERCENTAGE, FIXED_AMOUNT
    val value: Double,
    val minOrderAmount: Double = 0.0,
    val isActive: Boolean = true
)
