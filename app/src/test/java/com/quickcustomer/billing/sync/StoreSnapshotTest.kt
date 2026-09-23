package com.quickcustomer.billing.sync

import com.quickcustomer.billing.data.CategoryEntity
import com.quickcustomer.billing.data.ProductEntity
import com.quickcustomer.billing.data.ShopSettingsEntity
import com.quickcustomer.billing.data.SyncStatus
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreSnapshotTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun snapshotRoundTripPreservesStoreData() {
        val snapshot = StoreSnapshot(
            generatedAt = 1234L,
            users = emptyList(),
            products = listOf(
                ProductEntity(
                    id = "product-1",
                    category = "Coffee",
                    name = "Filter Coffee",
                    pricePaise = 2500,
                    sortOrder = 1,
                    syncStatus = SyncStatus.PENDING
                )
            ),
            categories = listOf(CategoryEntity("Coffee", 1)),
            sales = emptyList(),
            saleItems = emptyList(),
            settings = ShopSettingsEntity(shopName = "Test Store"),
            auditLogs = emptyList(),
            expenses = emptyList(),
            inventoryItems = emptyList(),
            stockTransactions = emptyList(),
            recipeIngredients = emptyList()
        )

        val restored = json.decodeFromString<StoreSnapshot>(json.encodeToString(snapshot))

        assertEquals(snapshot, restored)
        assertEquals("Test Store", restored.settings.shopName)
        assertEquals(2500L, restored.products.single().pricePaise)
    }

    @Test
    fun manifestRoundTripPreservesPrimaryDevice() {
        val manifest = DriveStoreManifest(
            storeId = "store-1",
            primaryDeviceId = "device-1",
            shopName = "Test Store",
            createdAt = 100,
            updatedAt = 200
        )

        val restored = json.decodeFromString<DriveStoreManifest>(json.encodeToString(manifest))

        assertEquals(manifest, restored)
    }
}
