package com.quickcustomer.billing.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        requireNotNull(AppDatabase::class.java.canonicalName),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2PreservesExistingSale() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO sales (
                    id, businessId, shopId, deviceId, invoiceNumber, createdAt,
                    cashierId, cashierName, subtotalPaise, discountPaise, taxPaise,
                    totalPaise, paymentMethod, isCancelled, syncStatus
                ) VALUES (
                    'sale-1', 'business-demo', 'shop-main', 'device-1', 'QC-TEST-1', 1000,
                    'cashier-1', 'Cashier', 2000, 0, 0, 2000, 'CASH', 0, 'PENDING'
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2).use { db ->
            db.query("SELECT invoiceNumber, cancelledAt FROM sales WHERE id = 'sale-1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("QC-TEST-1", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
            }
        }
    }

    @Test
    fun migrate2To3AddsCashAndUpiFieldsWithoutLosingSettings() {
        helper.createDatabase(TEST_DB_V2, 2).apply {
            execSQL(
                """
                INSERT INTO shop_settings (
                    id, shopName, address, phone, taxEnabled, taxRateBps,
                    pricesIncludeTax, receiptFooter, printerEnabled, updatedAt
                ) VALUES (
                    1, 'Quick Customer', '', '', 0, 0, 1, 'Thank you', 0, 1000
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_V2, 3, true, AppDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT shopName, upiQrImageUri FROM shop_settings WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Quick Customer", cursor.getString(0))
                assertEquals("", cursor.getString(1))
            }
        }
    }

    @Test
    fun migrate4To5AddsSafeProductRemovalAndCategoryOrdering() {
        helper.createDatabase(TEST_DB_V4, 4).apply {
            execSQL(
                """
                INSERT INTO products (
                    id, category, name, pricePaise, sortOrder, isActive, updatedAt, syncStatus
                ) VALUES (
                    'product-1', 'Teas', 'Dum Tea', 1200, 0, 1, 1000, 'PENDING'
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_V4, 5, true, AppDatabase.MIGRATION_4_5).use { db ->
            db.query("SELECT name, isDeleted FROM products WHERE id = 'product-1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Dum Tea", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
            }
            db.query("SELECT COUNT(*) FROM categories").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrate5To6AddsExpensesInventoryRecipesAndCostSnapshots() {
        helper.createDatabase(TEST_DB_V5, 5).apply {
            execSQL(
                """
                INSERT INTO sales (
                    id, businessId, shopId, deviceId, invoiceNumber, createdAt,
                    cashierId, cashierName, subtotalPaise, discountPaise, taxPaise,
                    totalPaise, paymentMethod, cashReceivedPaise, changeReturnedPaise,
                    isCancelled, cancelledAt, cancelledById, cancelledByName,
                    cancellationReason, syncStatus
                ) VALUES ('sale-v5', 'business-demo', 'shop-main', 'device-1', 'QC-V5', 1000,
                    'cashier', 'Cashier', 2000, 0, 0, 2000, 'CASH', 2000, 0,
                    0, NULL, NULL, NULL, NULL, 'PENDING')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO sale_items (id, saleId, productId, productNameSnapshot, unitPricePaise, quantity, lineTotalPaise)
                VALUES ('line-v5', 'sale-v5', 'product-1', 'Tea', 2000, 1, 2000)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_V5, 6, true, AppDatabase.MIGRATION_5_6).use { db ->
            db.query("SELECT costTotalPaise, costConfigured FROM sale_items WHERE id = 'line-v5'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getLong(0))
                assertEquals(0, cursor.getInt(1))
            }
            listOf("expenses", "inventory_items", "stock_transactions", "recipe_ingredients").forEach { table ->
                db.query("SELECT COUNT(*) FROM $table").use { cursor ->
                    cursor.moveToFirst()
                    assertEquals(0, cursor.getInt(0))
                }
            }
        }
    }

    companion object {
        private const val TEST_DB = "quick-customer-migration-v1-test"
        private const val TEST_DB_V2 = "quick-customer-migration-v2-test"
        private const val TEST_DB_V4 = "quick-customer-migration-v4-test"
        private const val TEST_DB_V5 = "quick-customer-migration-v5-test"
    }
}
