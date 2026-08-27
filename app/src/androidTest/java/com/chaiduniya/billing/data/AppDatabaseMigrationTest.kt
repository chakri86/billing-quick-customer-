package com.chaiduniya.billing.data

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
                    'sale-1', 'business-demo', 'shop-main', 'device-1', 'CD-TEST-1', 1000,
                    'cashier-1', 'Cashier', 2000, 0, 0, 2000, 'CASH', 0, 'PENDING'
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2).use { db ->
            db.query("SELECT invoiceNumber, cancelledAt FROM sales WHERE id = 'sale-1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("CD-TEST-1", cursor.getString(0))
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

    companion object {
        private const val TEST_DB = "chai-duniya-migration-test"
        private const val TEST_DB_V2 = "quick-customer-migration-v2-test"
    }
}
