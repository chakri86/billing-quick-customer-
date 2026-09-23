package com.quickcustomer.billing.data

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class HeldOrderRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: BillingRepository
    private val actor = UserEntity("owner", "owner", "Owner", UserRole.SUPER_USER, "", "")
    private val lines = listOf(
        CartLine(ProductEntity("tea", "Tea", "Tea", 2000, 0), 2),
        CartLine(ProductEntity("misc-one", "Misc", "Snack", 1500, 0), 1)
    )

    @Before fun setup() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("held-test")
        db = Room.databaseBuilder(context, AppDatabase::class.java, "held-test").allowMainThreadQueries().build()
        repository = BillingRepository(db)
    }
    @After fun close() { db.close() }

    @Test fun savedOrderSurvivesReopenAndDoesNotCreateSales() = runBlocking {
        val held = repository.holdOrder(null, "Table 2", actor, lines)
        db.close()
        db = Room.databaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java, "held-test")
            .allowMainThreadQueries().build()
        repository = BillingRepository(db)
        assertEquals(lines, repository.resumeOrder(held.id, actor).lines())
        assertTrue(repository.exportStoreSnapshot().sales.isEmpty())
        assertTrue(repository.exportStoreSnapshot().stockTransactions.isEmpty())
    }

    @Test fun failedPaymentRetainsHoldAndSuccessfulPaymentConsumesItOnce() = runBlocking {
        val held = repository.holdOrder(null, "", actor, lines)
        assertTrue(runCatching { pay(held.id, 1) }.isFailure)
        assertNotNull(db.heldOrderDao().get(held.id))
        assertTrue(repository.exportStoreSnapshot().sales.isEmpty())
        pay(held.id, 5500)
        assertNull(db.heldOrderDao().get(held.id))
        assertTrue(runCatching { pay(held.id, 5500) }.isFailure)
        assertEquals(1, repository.exportStoreSnapshot().sales.size)
    }

    @Test fun cancelledHoldRetainsItemsAndCannotResumeOrBecomeSale() = runBlocking {
        val held = repository.holdOrder(null, "Customer A", actor, lines)
        repository.cancelHeldOrder(held.id, actor, "Customer left")
        val cancelled = requireNotNull(db.heldOrderDao().get(held.id))
        assertEquals("CANCELLED", cancelled.status)
        assertEquals(lines, cancelled.lines())
        assertEquals("Customer left", cancelled.cancellationReason)
        assertTrue(runCatching { repository.resumeOrder(held.id, actor) }.isFailure)
        assertTrue(runCatching { pay(held.id, 5500) }.isFailure)
        assertTrue(repository.exportStoreSnapshot().sales.isEmpty())
        assertTrue(repository.exportStoreSnapshot().stockTransactions.isEmpty())
    }

    @Test fun reholdUpdatesSameOrderAndEmployeesCannotTakeOthersOrders() = runBlocking {
        val held = repository.holdOrder(null, "", actor, lines)
        val changed = lines.map { it.copy(quantity = 3) }
        repository.holdOrder(held.id, "Table 4", actor, changed)
        assertEquals(1, db.heldOrderDao().all().size)
        assertEquals(changed, repository.resumeOrder(held.id, actor).lines())
        val employee = actor.copy(id = "employee", role = UserRole.EMPLOYEE)
        assertTrue(runCatching { repository.resumeOrder(held.id, employee) }.isFailure)
        assertTrue(runCatching { repository.cancelHeldOrder(held.id, employee, "left") }.isFailure)
    }

    @Test fun migrationAddsHeldOrdersWithoutLosingExistingProducts() = runBlocking {
        db.productDao().insertAll(listOf(lines.first().product))
        // Simulate the previous schema: v9 differs from v8 only by this new table.
        db.openHelper.writableDatabase.apply {
            execSQL("DROP TABLE held_orders")
            version = 8
        }
        db.close()
        db = Room.databaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java, "held-test")
            .addMigrations(AppDatabase.MIGRATION_8_9).allowMainThreadQueries().build()
        repository = BillingRepository(db)
        assertEquals("Tea", repository.exportStoreSnapshot().products.single().name)
        assertTrue(db.heldOrderDao().all().isEmpty())
        repository.holdOrder(null, "After upgrade", actor, lines)
        assertEquals(1, db.heldOrderDao().all().size)
    }

    @Test fun snapshotsCarryHoldsAndRemoveConsumedOrdersOnMonitor() = runBlocking {
        val held = repository.holdOrder(null, "Sync", actor, lines)
        val saved = repository.exportStoreSnapshot()
        assertEquals(lines, saved.heldOrders.single().lines())
        repository.cancelHeldOrder(held.id, actor, "Customer left")
        val cancelled = repository.exportStoreSnapshot()
        repository.importStoreSnapshot(saved)
        assertEquals("HELD", db.heldOrderDao().get(held.id)?.status)
        repository.importStoreSnapshot(cancelled)
        assertEquals("CANCELLED", db.heldOrderDao().get(held.id)?.status)
        repository.importStoreSnapshot(saved.copy(heldOrders = emptyList()))
        assertTrue(db.heldOrderDao().all().isEmpty())
    }

    private suspend fun pay(id: String, cash: Long) = repository.completeSale(
        actor, lines, PaymentMethod.CASH, 0, cash, ShopSettingsEntity(), heldOrderId = id
    )
}
