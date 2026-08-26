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
    @Query("SELECT * FROM products ORDER BY category, sortOrder, name")
    fun observeAll(): Flow<List<ProductEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(products: List<ProductEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(product: ProductEntity)
    @Update suspend fun update(product: ProductEntity)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY createdAt DESC") fun observeAll(): Flow<List<SaleEntity>>
    @Query("SELECT COUNT(*) FROM sales WHERE syncStatus != 'SYNCED'") fun observePendingCount(): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSale(sale: SaleEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertItems(items: List<SaleItemEntity>)
}

@Database(
    entities = [UserEntity::class, ProductEntity::class, SaleEntity::class, SaleItemEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(DbConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "chai-duniya-billing.db"
            ).build().also { instance = it }
        }
    }
}
