package com.chaiduniya.billing.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole { SUPER_USER, ADMIN, EMPLOYEE }
enum class PaymentMethod { CASH, UPI, CARD }
enum class SyncStatus { PENDING, SYNCED, FAILED }

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val role: UserRole,
    val passwordSalt: String,
    val passwordHash: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products", indices = [Index(value = ["category", "sortOrder"])])
data class ProductEntity(
    @PrimaryKey val id: String,
    val category: String,
    val name: String,
    val pricePaise: Long,
    val sortOrder: Int,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(tableName = "sales", indices = [Index(value = ["invoiceNumber"], unique = true)])
data class SaleEntity(
    @PrimaryKey val id: String,
    val businessId: String = "business-demo",
    val shopId: String = "shop-main",
    val deviceId: String = "local-device",
    val invoiceNumber: String,
    val createdAt: Long,
    val cashierId: String,
    val cashierName: String,
    val subtotalPaise: Long,
    val discountPaise: Long = 0,
    val taxPaise: Long = 0,
    val totalPaise: Long,
    val paymentMethod: PaymentMethod,
    val isCancelled: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId"), Index("productId")]
)
data class SaleItemEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val productId: String,
    val productNameSnapshot: String,
    val unitPricePaise: Long,
    val quantity: Int,
    val lineTotalPaise: Long
)

data class CartLine(
    val product: ProductEntity,
    val quantity: Int
) {
    val lineTotalPaise: Long get() = product.pricePaise * quantity
}

data class Receipt(
    val sale: SaleEntity,
    val lines: List<CartLine>
)
