package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class POSConverters {
    private val moshi = Moshi.Builder().build()
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)

    @TypeConverter
    fun fromTableStatus(value: TableStatus): String = value.name

    @TypeConverter
    fun toTableStatus(value: String): TableStatus = TableStatus.valueOf(value)

    @TypeConverter
    fun fromOrderType(value: OrderType): String = value.name

    @TypeConverter
    fun toOrderType(value: String): OrderType = OrderType.valueOf(value)

    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = OrderStatus.valueOf(value)

    @TypeConverter
    fun fromRole(value: Role): String = value.name

    @TypeConverter
    fun toRole(value: String): Role = Role.valueOf(value)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY isFavorite DESC, name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Query("UPDATE products SET stockQuantity = :newStock WHERE id = :id")
    suspend fun updateStock(id: String, newStock: Double)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: String)

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()
}

@Dao
interface TableDao {
    @Query("SELECT * FROM tables ORDER BY name ASC")
    fun getAllTables(): Flow<List<Table>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: Table)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTables(tables: List<Table>)

    @Query("UPDATE tables SET status = :status WHERE id = :id")
    suspend fun updateTableStatus(id: String, status: TableStatus)

    @Query("DELETE FROM tables WHERE id = :id")
    suspend fun deleteTable(id: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE isSynced = 0")
    suspend fun getUnsyncedOrders(): List<Order>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): Order?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<Order>)

    @Query("UPDATE orders SET status = :status, isSynced = 0 WHERE id = :id")
    suspend fun updateOrderStatus(id: String, status: OrderStatus)

    @Query("UPDATE orders SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrder(id: String)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE status = 'OPEN' LIMIT 1")
    fun getOpenShift(): Flow<Shift?>

    @Query("SELECT * FROM shifts ORDER BY openTime DESC")
    fun getAllShifts(): Flow<List<Shift>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: Shift)

    @Query("UPDATE shifts SET closeTime = :closeTime, closingCash = :closingCash, status = 'CLOSED' WHERE id = :id")
    suspend fun closeShift(id: String, closeTime: Long, closingCash: Double)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE pin = :pin LIMIT 1")
    suspend fun getUserByPin(pin: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: String)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    fun getQueueFlow(): Flow<List<SyncQueue>>

    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getQueue(): List<SyncQueue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(item: SyncQueue)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteQueueItem(id: Long)

    @Query("DELETE FROM sync_queue")
    suspend fun clearQueue()
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier)
}

@Dao
interface PromotionDao {
    @Query("SELECT * FROM promotions ORDER BY name ASC")
    fun getAllPromotions(): Flow<List<Promotion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotion(promotion: Promotion)
}

@Database(
    entities = [
        Product::class,
        Table::class,
        Order::class,
        Shift::class,
        User::class,
        SyncQueue::class,
        AuditLog::class,
        Supplier::class,
        Promotion::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(POSConverters::class)
abstract class POSDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun tableDao(): TableDao
    abstract fun orderDao(): OrderDao
    abstract fun shiftDao(): ShiftDao
    abstract fun userDao(): UserDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun auditDao(): AuditDao
    abstract fun supplierDao(): SupplierDao
    abstract fun promotionDao(): PromotionDao
}
