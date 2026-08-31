package com.chaiduniya.billing.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole { SUPER_USER, ADMIN, EMPLOYEE }
enum class PaymentMethod { CASH, UPI, CARD }
enum class SyncStatus { PENDING, SYNCED, FAILED }
enum class ExpenseStatus { PENDING, APPROVED, REJECTED, CANCELLED }
enum class InventoryUnit { PIECE, PACKET, GRAM, KILOGRAM, MILLILITRE, LITRE, BOTTLE, BOX }
enum class StockTransactionType { OPENING, PURCHASE, SALE, WASTAGE, SUPPLIER_RETURN, ADJUSTMENT, SALE_CANCELLED }

object BillingCategories {
    const val MISC = "Misc"
}

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
    @ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(tableName = "categories", indices = [Index(value = ["sortOrder"])])
data class CategoryEntity(
    @PrimaryKey val name: String,
    val sortOrder: Int,
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
    val cashReceivedPaise: Long? = null,
    val changeReturnedPaise: Long? = null,
    val isCancelled: Boolean = false,
    val cancelledAt: Long? = null,
    val cancelledById: String? = null,
    val cancelledByName: String? = null,
    val cancellationReason: String? = null,
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
    val lineTotalPaise: Long,
    @ColumnInfo(defaultValue = "0") val costTotalPaise: Long = 0,
    @ColumnInfo(defaultValue = "0") val costConfigured: Boolean = false
)

@Entity(tableName = "expenses", indices = [Index("occurredAt"), Index("status"), Index("enteredById")])
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val category: String,
    val amountPaise: Long,
    val occurredAt: Long,
    val paymentMethod: PaymentMethod,
    val supplierName: String = "",
    val description: String = "",
    val enteredById: String,
    val enteredByName: String,
    val status: ExpenseStatus,
    val approvedById: String? = null,
    val approvedByName: String? = null,
    val approvedAt: Long? = null,
    val rejectedById: String? = null,
    val rejectedByName: String? = null,
    val rejectedAt: Long? = null,
    val rejectionReason: String? = null,
    val cancelledById: String? = null,
    val cancelledByName: String? = null,
    val cancelledAt: Long? = null,
    val cancellationReason: String? = null,
    val linkedStockTransactionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(tableName = "inventory_items", indices = [Index(value = ["name"], unique = true)])
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val unit: InventoryUnit,
    val minimumStockMilli: Long = 0,
    val averageCostPaisePerUnit: Long = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(
    tableName = "stock_transactions",
    foreignKeys = [ForeignKey(
        entity = InventoryItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["inventoryItemId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("inventoryItemId"), Index("saleId"), Index("expenseId"), Index("createdAt")]
)
data class StockTransactionEntity(
    @PrimaryKey val id: String,
    val inventoryItemId: String,
    val type: StockTransactionType,
    val quantityDeltaMilli: Long,
    val totalCostPaise: Long = 0,
    val supplierName: String = "",
    val description: String = "",
    val saleId: String? = null,
    val expenseId: String? = null,
    val actorId: String,
    val actorName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

@Entity(
    tableName = "recipe_ingredients",
    primaryKeys = ["productId", "inventoryItemId"],
    indices = [Index("inventoryItemId")]
)
data class RecipeIngredientEntity(
    val productId: String,
    val inventoryItemId: String,
    val quantityMilliPerSaleUnit: Long,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

data class InventoryStock(
    @androidx.room.Embedded val item: InventoryItemEntity,
    val currentStockMilli: Long
)

data class RecipeIngredientDetail(
    val productId: String,
    val inventoryItemId: String,
    val quantityMilliPerSaleUnit: Long,
    val inventoryName: String,
    val unit: InventoryUnit,
    val averageCostPaisePerUnit: Long
)

data class CartLine(
    val product: ProductEntity,
    val quantity: Int
) {
    val lineTotalPaise: Long get() = product.pricePaise * quantity
}

data class Receipt(
    val sale: SaleEntity,
    val lines: List<CartLine>,
    val settings: ShopSettingsEntity
)

data class BillDetails(
    val sale: SaleEntity,
    val items: List<SaleItemEntity>,
    val settings: ShopSettingsEntity
)

@Entity(tableName = "shop_settings")
data class ShopSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val shopName: String = "Quick Customer",
    val address: String = "",
    val phone: String = "",
    val taxEnabled: Boolean = false,
    val taxRateBps: Int = 0,
    val pricesIncludeTax: Boolean = true,
    val receiptFooter: String = "Thank you. Visit again!",
    val printerEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "''") val printerName: String = "",
    @ColumnInfo(defaultValue = "''") val printerAddress: String = "",
    @ColumnInfo(defaultValue = "58") val printerPaperWidthMm: Int = 58,
    @ColumnInfo(defaultValue = "0") val printerAutoPrint: Boolean = false,
    @ColumnInfo(defaultValue = "''") val upiQrImageUri: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs", indices = [Index("entityId"), Index("createdAt")])
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val actorId: String,
    val actorName: String,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

data class ProductSalesSummary(
    val productName: String,
    val quantity: Long,
    val revenuePaise: Long
)

data class ProductProfitSummary(
    val productName: String,
    val quantity: Long,
    val revenuePaise: Long,
    val costPaise: Long,
    val costConfiguredCount: Long,
    val lineCount: Long
)

object ExpenseCategories {
    val defaults = listOf(
        "Milk and dairy", "Tea/coffee materials", "Sugar", "Snacks purchases",
        "Cups and packaging", "Gas", "Electricity", "Rent", "Employee wages",
        "Maintenance", "Transportation", "Other"
    )
}
