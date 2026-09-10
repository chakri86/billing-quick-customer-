package com.quickcustomer.billing.sync

import com.quickcustomer.billing.data.AuditLogEntity
import com.quickcustomer.billing.data.CategoryEntity
import com.quickcustomer.billing.data.ExpenseEntity
import com.quickcustomer.billing.data.InventoryItemEntity
import com.quickcustomer.billing.data.ProductEntity
import com.quickcustomer.billing.data.RecipeIngredientEntity
import com.quickcustomer.billing.data.SaleEntity
import com.quickcustomer.billing.data.SaleItemEntity
import com.quickcustomer.billing.data.ShopSettingsEntity
import com.quickcustomer.billing.data.StockTransactionEntity
import com.quickcustomer.billing.data.UserEntity
import kotlinx.serialization.Serializable

@Serializable
data class StoreSnapshot(
    val formatVersion: Int = CURRENT_FORMAT,
    val generatedAt: Long = System.currentTimeMillis(),
    val users: List<UserEntity>,
    val products: List<ProductEntity>,
    val categories: List<CategoryEntity>,
    val sales: List<SaleEntity>,
    val saleItems: List<SaleItemEntity>,
    val settings: ShopSettingsEntity,
    val auditLogs: List<AuditLogEntity>,
    val expenses: List<ExpenseEntity>,
    val inventoryItems: List<InventoryItemEntity>,
    val stockTransactions: List<StockTransactionEntity>,
    val recipeIngredients: List<RecipeIngredientEntity>
) {
    init {
        require(formatVersion in 1..CURRENT_FORMAT) { "Unsupported store snapshot format." }
    }

    companion object {
        const val CURRENT_FORMAT = 1
    }
}
