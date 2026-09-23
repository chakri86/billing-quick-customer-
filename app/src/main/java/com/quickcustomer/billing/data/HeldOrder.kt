package com.quickcustomer.billing.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
@Entity(tableName = "held_orders")
data class HeldOrder(
    @PrimaryKey val id: String,
    val label: String,
    val cashierId: String,
    val cashierName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val linesJson: String,
    val status: String = "HELD",
    val cancellationReason: String = "",
    val cancelledByName: String = ""
) {
    fun lines(): List<CartLine> = Json.decodeFromString<List<HeldLine>>(linesJson)
        .map { CartLine(it.product, it.quantity) }

    companion object {
        fun encode(lines: List<CartLine>): String {
            require(lines.isNotEmpty() && lines.all { it.quantity > 0 && it.product.pricePaise >= 0 }) {
                "A held order must contain valid items."
            }
            return Json.encodeToString(lines.map { HeldLine(it.product, it.quantity) })
        }
    }
}

@Serializable
private data class HeldLine(val product: ProductEntity, val quantity: Int)

@Dao
interface HeldOrderDao {
    @Query("SELECT * FROM held_orders ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<HeldOrder>>
    @Query("SELECT * FROM held_orders") suspend fun all(): List<HeldOrder>
    @Query("SELECT * FROM held_orders WHERE id = :id") suspend fun get(id: String): HeldOrder?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(order: HeldOrder)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(orders: List<HeldOrder>)
    @Query("DELETE FROM held_orders") suspend fun deleteAll()
    @Query("DELETE FROM held_orders WHERE id = :id AND status = 'HELD'")
    suspend fun consume(id: String): Int
}
