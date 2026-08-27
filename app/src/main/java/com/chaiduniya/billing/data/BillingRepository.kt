package com.chaiduniya.billing.data

import androidx.room.withTransaction
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import com.chaiduniya.billing.domain.BillingCalculator
import kotlinx.coroutines.flow.Flow

class BillingRepository(private val db: AppDatabase) {
    val products: Flow<List<ProductEntity>> = db.productDao().observeAll()
    val users: Flow<List<UserEntity>> = db.userDao().observeAll()
    val sales: Flow<List<SaleEntity>> = db.saleDao().observeAll()
    val pendingSyncCount: Flow<Int> = db.saleDao().observePendingCount()
    val settings: Flow<ShopSettingsEntity?> = db.settingsDao().observe()
    val productSales: Flow<List<ProductSalesSummary>> = db.saleDao().observeProductSales()
    val auditLogs: Flow<List<AuditLogEntity>> = db.auditDao().observeAll()

    suspend fun ensureSeeded() = db.withTransaction {
        if (db.userDao().count() == 0) {
            db.userDao().insertAll(
                listOf(
                    newUser("owner", "Shop Owner", UserRole.SUPER_USER, "Owner@123"),
                    newUser("admin", "Shop Manager", UserRole.ADMIN, "Admin@123"),
                    newUser("cashier", "Cashier", UserRole.EMPLOYEE, "Cashier@123")
                )
            )
        }
        if (db.productDao().count() == 0) {
            db.productDao().insertAll(SeedData.products())
        } else {
            db.productDao().renameBrand(
                oldName = "Chai Duniya Spl Tea",
                newName = "Quick Customer Spl Tea",
                oldCategory = "Chai Duniya Special Shakes",
                newCategory = "Quick Customer Special Shakes",
                updatedAt = System.currentTimeMillis()
            )
        }
        if (db.settingsDao().count() == 0) db.settingsDao().save(ShopSettingsEntity())
        else db.settingsDao().renameDefaultShop("Chai Duniya", "Quick Customer", System.currentTimeMillis())
    }

    suspend fun authenticate(username: String, password: String): UserEntity? {
        val user = db.userDao().findByUsername(username.trim()) ?: return null
        if (!user.isActive) return null
        val candidate = PasswordHasher.hash(password, user.passwordSalt)
        return user.takeIf {
            MessageDigest.isEqual(candidate.toByteArray(), user.passwordHash.toByteArray())
        }
    }

    suspend fun addUser(username: String, displayName: String, role: UserRole, password: String) {
        db.userDao().insert(newUser(username.trim(), displayName.trim(), role, password))
    }

    suspend fun updateUser(user: UserEntity) = db.userDao().update(user)

    suspend fun addProduct(name: String, category: String, priceRupees: Long) {
        db.productDao().insert(
            ProductEntity(
                id = UUID.randomUUID().toString(),
                category = category.trim(),
                name = name.trim(),
                pricePaise = priceRupees * 100,
                sortOrder = Int.MAX_VALUE
            )
        )
    }

    suspend fun updateProduct(product: ProductEntity) = db.productDao().update(
        product.copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING)
    )

    suspend fun completeSale(
        cashier: UserEntity,
        lines: List<CartLine>,
        paymentMethod: PaymentMethod,
        requestedDiscountPaise: Long,
        cashReceivedPaise: Long?,
        settings: ShopSettingsEntity
    ): Receipt = db.withTransaction {
        require(lines.isNotEmpty()) { "A bill must contain at least one item." }
        val now = System.currentTimeMillis()
        val saleId = UUID.randomUUID().toString()
        val subtotal = lines.sumOf { it.lineTotalPaise }
        val totals = BillingCalculator.calculate(
            subtotalPaise = subtotal,
            requestedDiscountPaise = requestedDiscountPaise,
            taxEnabled = settings.taxEnabled,
            taxRateBps = settings.taxRateBps,
            pricesIncludeTax = settings.pricesIncludeTax
        )
        val received = cashReceivedPaise.takeIf { paymentMethod == PaymentMethod.CASH }
        if (paymentMethod == PaymentMethod.CASH) {
            require(received != null && received >= totals.totalPaise) {
                "Cash received must be at least the amount due."
            }
        }
        val invoiceNumber = buildInvoiceNumber(now)
        val sale = SaleEntity(
            id = saleId,
            invoiceNumber = invoiceNumber,
            createdAt = now,
            cashierId = cashier.id,
            cashierName = cashier.displayName,
            subtotalPaise = totals.subtotalPaise,
            discountPaise = totals.discountPaise,
            taxPaise = totals.taxPaise,
            totalPaise = totals.totalPaise,
            paymentMethod = paymentMethod,
            cashReceivedPaise = received,
            changeReturnedPaise = received?.minus(totals.totalPaise)
        )
        db.saleDao().insertSale(sale)
        db.saleDao().insertItems(lines.map { line ->
            SaleItemEntity(
                id = UUID.randomUUID().toString(),
                saleId = saleId,
                productId = line.product.id,
                productNameSnapshot = line.product.name,
                unitPricePaise = line.product.pricePaise,
                quantity = line.quantity,
                lineTotalPaise = line.lineTotalPaise
            )
        })
        Receipt(sale, lines, settings)
    }

    suspend fun cancelSale(sale: SaleEntity, actor: UserEntity, reason: String) = db.withTransaction {
        require(actor.role != UserRole.EMPLOYEE) { "Admin or Super User access is required." }
        require(reason.trim().length >= 3) { "Enter a cancellation reason." }
        val changed = db.saleDao().cancel(
            saleId = sale.id,
            cancelledAt = System.currentTimeMillis(),
            cancelledById = actor.id,
            cancelledByName = actor.displayName,
            reason = reason.trim()
        )
        require(changed == 1) { "This bill is already cancelled or no longer available." }
        db.auditDao().insert(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = "SALE_CANCELLED",
                entityType = "SALE",
                entityId = sale.id,
                actorId = actor.id,
                actorName = actor.displayName,
                reason = reason.trim()
            )
        )
    }

    suspend fun saveSettings(settings: ShopSettingsEntity, actor: UserEntity) = db.withTransaction {
        require(actor.role == UserRole.SUPER_USER) { "Only the Super User can change shop settings." }
        val safe = settings.copy(
            id = 1,
            shopName = settings.shopName.trim().ifBlank { "Quick Customer" },
            taxRateBps = settings.taxRateBps.coerceIn(0, 10_000),
            updatedAt = System.currentTimeMillis()
        )
        db.settingsDao().save(safe)
        db.auditDao().insert(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = "SHOP_SETTINGS_UPDATED",
                entityType = "SHOP_SETTINGS",
                entityId = "1",
                actorId = actor.id,
                actorName = actor.displayName,
                reason = "Shop settings updated"
            )
        )
    }

    private fun newUser(
        username: String,
        displayName: String,
        role: UserRole,
        password: String
    ): UserEntity {
        val salt = PasswordHasher.newSalt()
        return UserEntity(
            id = UUID.randomUUID().toString(),
            username = username.lowercase(),
            displayName = displayName,
            role = role,
            passwordSalt = salt,
            passwordHash = PasswordHasher.hash(password, salt)
        )
    }

    private fun buildInvoiceNumber(time: Long): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(time))
        val suffix = UUID.randomUUID().toString().take(4).uppercase()
        return "CD-$stamp-$suffix"
    }
}

private object PasswordHasher {
    private const val iterations = 120_000
    private const val keyLength = 256

    fun newSalt(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(password: String, saltHex: String): String {
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
        spec.clearPassword()
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
