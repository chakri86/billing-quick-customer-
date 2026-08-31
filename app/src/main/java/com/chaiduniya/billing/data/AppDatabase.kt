package com.chaiduniya.billing.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

class DbConverters {
    @TypeConverter fun roleToString(value: UserRole): String = value.name
    @TypeConverter fun stringToRole(value: String): UserRole = UserRole.valueOf(value)
    @TypeConverter fun paymentToString(value: PaymentMethod): String = value.name
    @TypeConverter fun stringToPayment(value: String): PaymentMethod = PaymentMethod.valueOf(value)
    @TypeConverter fun syncToString(value: SyncStatus): String = value.name
    @TypeConverter fun stringToSync(value: String): SyncStatus = SyncStatus.valueOf(value)
}

@Dao
interface UserDao {
    @Query("SELECT COUNT(*) FROM users") suspend fun count(): Int
    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?
    @Query("SELECT * FROM users ORDER BY role, displayName") fun observeAll(): Flow<List<UserEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(user: UserEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(users: List<UserEntity>)
    @Update suspend fun update(user: UserEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT COUNT(*) FROM products") suspend fun count(): Int
    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY category, sortOrder, name")
    fun observeAll(): Flow<List<ProductEntity>>
    @Query("SELECT DISTINCT category FROM products WHERE isDeleted = 0 ORDER BY category")
    suspend fun categoryNames(): List<String>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(products: List<ProductEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(product: ProductEntity)
    @Update suspend fun update(product: ProductEntity)
    @Query(
        """
        UPDATE products
        SET name = CASE WHEN name = :oldName THEN :newName ELSE name END,
            category = CASE WHEN category = :oldCategory THEN :newCategory ELSE category END,
            updatedAt = :updatedAt,
            syncStatus = 'PENDING'
        WHERE name = :oldName OR category = :oldCategory
        """
    )
    suspend fun renameBrand(
        oldName: String,
        newName: String,
        oldCategory: String,
        newCategory: String,
        updatedAt: Long
    )
    @Query(
        "UPDATE products SET category = :newCategory, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE category = :oldCategory"
    )
    suspend fun renameCategory(oldCategory: String, newCategory: String, updatedAt: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT COUNT(*) FROM categories") suspend fun count(): Int
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM categories") suspend fun maxSortOrder(): Int
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(category: CategoryEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(categories: List<CategoryEntity>)
    @Query(
        "UPDATE categories SET sortOrder = :sortOrder, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE name = :name"
    )
    suspend fun updateOrder(name: String, sortOrder: Int, updatedAt: Long)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY createdAt DESC") fun observeAll(): Flow<List<SaleEntity>>
    @Query("SELECT COUNT(*) FROM sales WHERE syncStatus != 'SYNCED'") fun observePendingCount(): Flow<Int>
    @Query("SELECT * FROM sale_items WHERE saleId = :saleId ORDER BY rowid")
    suspend fun itemsForSale(saleId: String): List<SaleItemEntity>
    @Query(
        """
        SELECT si.productNameSnapshot AS productName,
               SUM(si.quantity) AS quantity,
               SUM(si.lineTotalPaise) AS revenuePaise
        FROM sale_items si
        INNER JOIN sales s ON s.id = si.saleId
        WHERE s.isCancelled = 0
        GROUP BY si.productNameSnapshot
        ORDER BY quantity DESC, productName ASC
        """
    )
    fun observeProductSales(): Flow<List<ProductSalesSummary>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSale(sale: SaleEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertItems(items: List<SaleItemEntity>)
    @Query(
        """
        UPDATE sales
        SET isCancelled = 1,
            cancelledAt = :cancelledAt,
            cancelledById = :cancelledById,
            cancelledByName = :cancelledByName,
            cancellationReason = :reason,
            syncStatus = 'PENDING'
        WHERE id = :saleId AND isCancelled = 0
        """
    )
    suspend fun cancel(
        saleId: String,
        cancelledAt: Long,
        cancelledById: String,
        cancelledByName: String,
        reason: String
    ): Int
}

@Dao
interface SettingsDao {
    @Query("SELECT COUNT(*) FROM shop_settings") suspend fun count(): Int
    @Query("SELECT * FROM shop_settings WHERE id = 1") fun observe(): Flow<ShopSettingsEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(settings: ShopSettingsEntity)
    @Query(
        "UPDATE shop_settings SET shopName = :newName, updatedAt = :updatedAt WHERE id = 1 AND shopName = :oldName"
    )
    suspend fun renameDefaultShop(oldName: String, newName: String, updatedAt: Long)
}

@Dao
interface AuditDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(entry: AuditLogEntity)
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC") fun observeAll(): Flow<List<AuditLogEntity>>
}

@Database(
    entities = [
        UserEntity::class,
        ProductEntity::class,
        CategoryEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        ShopSettingsEntity::class,
        AuditLogEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(DbConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleDao(): SaleDao
    abstract fun settingsDao(): SettingsDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "chai-duniya-billing.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                .also { instance = it }
        }

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales ADD COLUMN cancelledAt INTEGER")
                db.execSQL("ALTER TABLE sales ADD COLUMN cancelledById TEXT")
                db.execSQL("ALTER TABLE sales ADD COLUMN cancelledByName TEXT")
                db.execSQL("ALTER TABLE sales ADD COLUMN cancellationReason TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shop_settings (
                        id INTEGER NOT NULL,
                        shopName TEXT NOT NULL,
                        address TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        taxEnabled INTEGER NOT NULL,
                        taxRateBps INTEGER NOT NULL,
                        pricesIncludeTax INTEGER NOT NULL,
                        receiptFooter TEXT NOT NULL,
                        printerEnabled INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS audit_logs (
                        id TEXT NOT NULL,
                        action TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        actorId TEXT NOT NULL,
                        actorName TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_entityId ON audit_logs(entityId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_createdAt ON audit_logs(createdAt)")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales ADD COLUMN cashReceivedPaise INTEGER")
                db.execSQL("ALTER TABLE sales ADD COLUMN changeReturnedPaise INTEGER")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN upiQrImageUri TEXT NOT NULL DEFAULT ''")
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN printerName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN printerAddress TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN printerPaperWidthMm INTEGER NOT NULL DEFAULT 58")
                db.execSQL("ALTER TABLE shop_settings ADD COLUMN printerAutoPrint INTEGER NOT NULL DEFAULT 0")
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS categories (
                        name TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL,
                        PRIMARY KEY(name)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_sortOrder ON categories(sortOrder)")
            }
        }
    }
}
