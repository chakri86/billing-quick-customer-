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
import kotlinx.coroutines.flow.Flow

class BillingRepository(private val db: AppDatabase) {
    val products: Flow<List<ProductEntity>> = db.productDao().observeAll()
    val users: Flow<List<UserEntity>> = db.userDao().observeAll()
    val sales: Flow<List<SaleEntity>> = db.saleDao().observeAll()
    val pendingSyncCount: Flow<Int> = db.saleDao().observePendingCount()

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
        if (db.productDao().count() == 0) db.productDao().insertAll(SeedData.products())
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
        paymentMethod: PaymentMethod
    ): Receipt = db.withTransaction {
        require(lines.isNotEmpty()) { "A bill must contain at least one item." }
        val now = System.currentTimeMillis()
        val saleId = UUID.randomUUID().toString()
        val total = lines.sumOf { it.lineTotalPaise }
        val invoiceNumber = buildInvoiceNumber(now)
        val sale = SaleEntity(
            id = saleId,
            invoiceNumber = invoiceNumber,
            createdAt = now,
            cashierId = cashier.id,
            cashierName = cashier.displayName,
            subtotalPaise = total,
            totalPaise = total,
            paymentMethod = paymentMethod
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
        Receipt(sale, lines)
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
