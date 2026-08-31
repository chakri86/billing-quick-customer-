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
    val categories: Flow<List<CategoryEntity>> = db.categoryDao().observeAll()
    val users: Flow<List<UserEntity>> = db.userDao().observeAll()
    val sales: Flow<List<SaleEntity>> = db.saleDao().observeAll()
    val pendingSyncCount: Flow<Int> = db.saleDao().observePendingCount()
    val settings: Flow<ShopSettingsEntity?> = db.settingsDao().observe()
    val productSales: Flow<List<ProductSalesSummary>> = db.saleDao().observeProductSales()
    val productProfit: Flow<List<ProductProfitSummary>> = db.saleDao().observeProductProfit()
    val auditLogs: Flow<List<AuditLogEntity>> = db.auditDao().observeAll()
    val expenses: Flow<List<ExpenseEntity>> = db.expenseDao().observeAll()
    val inventoryStock: Flow<List<InventoryStock>> = db.inventoryDao().observeStock()
    val stockTransactions: Flow<List<StockTransactionEntity>> = db.inventoryDao().observeTransactions()
    val recipeDetails: Flow<List<RecipeIngredientDetail>> = db.inventoryDao().observeRecipeDetails()

    suspend fun ensureSeeded() = db.withTransaction {
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
        db.productDao().renameCategory(
            oldCategory = "Quick Customer Special Shakes",
            newCategory = "Special Shakes",
            updatedAt = System.currentTimeMillis()
        )
        ensureCategories()
        if (db.settingsDao().count() == 0) db.settingsDao().save(ShopSettingsEntity())
        else db.settingsDao().renameDefaultShop("Chai Duniya", "Quick Customer", System.currentTimeMillis())
    }

    suspend fun hasUsers(): Boolean = db.userDao().count() > 0

    suspend fun createInitialOwner(
        username: String,
        displayName: String,
        password: String
    ): UserEntity = db.withTransaction {
        require(username.trim().length >= 3) { "Username must contain at least 3 characters." }
        require(displayName.isNotBlank()) { "Display name is required." }
        require(password.length >= 8) { "Password must contain at least 8 characters." }
        check(db.userDao().count() == 0) { "An owner account already exists." }
        val owner = newUser(username.trim(), displayName.trim(), UserRole.SUPER_USER, password)
        db.userDao().insert(owner)
        owner
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

    suspend fun addProduct(name: String, category: String, priceRupees: Long) = db.withTransaction {
        require(name.trim().isNotBlank()) { "Product name is required." }
        require(category.trim().isNotBlank()) { "Category is required." }
        require(category.trim() != BillingCategories.MISC) { "Misc is reserved for custom bill items." }
        require(priceRupees > 0) { "Price must be greater than zero." }
        ensureCategory(category.trim())
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

    suspend fun updateProduct(product: ProductEntity) = db.withTransaction {
        require(product.name.trim().isNotBlank()) { "Product name is required." }
        require(product.category.trim().isNotBlank()) { "Category is required." }
        require(product.category.trim() != BillingCategories.MISC) { "Misc is reserved for custom bill items." }
        require(product.pricePaise > 0) { "Price must be greater than zero." }
        ensureCategory(product.category.trim())
        db.productDao().update(
            product.copy(
                name = product.name.trim(),
                category = product.category.trim(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
        )
    }

    suspend fun deleteProduct(product: ProductEntity) = db.productDao().update(
        product.copy(
            isActive = false,
            isDeleted = true,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
    )

    suspend fun saveCategoryOrder(names: List<String>) = db.withTransaction {
        val now = System.currentTimeMillis()
        names.distinct().forEachIndexed { index, name ->
            db.categoryDao().updateOrder(name, index, now)
        }
    }

    suspend fun billDetails(sale: SaleEntity, settings: ShopSettingsEntity): BillDetails =
        BillDetails(sale, db.saleDao().itemsForSale(sale.id), settings)

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
        val normalLines = lines.filterNot { it.product.id.startsWith("misc-") }
        val recipes = db.inventoryDao().recipesForProducts(normalLines.map { it.product.id })
        val inventoryById = db.inventoryDao().itemsByIds(recipes.map { it.inventoryItemId }.distinct())
            .associateBy { it.id }
        val recipesByProduct = recipes.groupBy { it.productId }
        db.saleDao().insertItems(lines.map { line ->
            val productRecipe = recipesByProduct[line.product.id].orEmpty()
            val unitCost = productRecipe.sumOf { ingredient ->
                val averageCost = inventoryById[ingredient.inventoryItemId]?.averageCostPaisePerUnit ?: 0
                ingredient.quantityMilliPerSaleUnit * averageCost / 1_000
            }
            SaleItemEntity(
                id = UUID.randomUUID().toString(),
                saleId = saleId,
                productId = line.product.id,
                productNameSnapshot = line.product.name,
                unitPricePaise = line.product.pricePaise,
                quantity = line.quantity,
                lineTotalPaise = line.lineTotalPaise,
                costTotalPaise = unitCost * line.quantity,
                costConfigured = productRecipe.isNotEmpty()
            )
        })
        val stockChanges = normalLines.flatMap { line ->
            recipesByProduct[line.product.id].orEmpty().map { ingredient ->
                StockTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    inventoryItemId = ingredient.inventoryItemId,
                    type = StockTransactionType.SALE,
                    quantityDeltaMilli = -(ingredient.quantityMilliPerSaleUnit * line.quantity),
                    saleId = saleId,
                    actorId = cashier.id,
                    actorName = cashier.displayName,
                    createdAt = now
                )
            }
        }
        if (stockChanges.isNotEmpty()) db.inventoryDao().insertTransactions(stockChanges)
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
        val saleTransactions = db.inventoryDao().saleTransactions(sale.id)
        if (saleTransactions.isNotEmpty()) {
            db.inventoryDao().insertTransactions(saleTransactions.map { original ->
                StockTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    inventoryItemId = original.inventoryItemId,
                    type = StockTransactionType.SALE_CANCELLED,
                    quantityDeltaMilli = -original.quantityDeltaMilli,
                    description = "Stock restored for ${sale.invoiceNumber}",
                    saleId = sale.id,
                    actorId = actor.id,
                    actorName = actor.displayName
                )
            })
        }
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
            printerName = settings.printerName.trim(),
            printerAddress = settings.printerAddress.trim().uppercase(Locale.US),
            printerPaperWidthMm = if (settings.printerPaperWidthMm >= 80) 80 else 58,
            printerAutoPrint = settings.printerEnabled && settings.printerAutoPrint,
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

    suspend fun addExpense(
        actor: UserEntity,
        category: String,
        amountPaise: Long,
        paymentMethod: PaymentMethod,
        supplierName: String,
        description: String
    ) = db.withTransaction {
        require(amountPaise > 0) { "Expense amount must be greater than zero." }
        require(category.isNotBlank()) { "Choose an expense category." }
        val approved = actor.role != UserRole.EMPLOYEE
        val now = System.currentTimeMillis()
        db.expenseDao().insert(
            ExpenseEntity(
                id = UUID.randomUUID().toString(),
                category = category.trim(),
                amountPaise = amountPaise,
                occurredAt = now,
                paymentMethod = paymentMethod,
                supplierName = supplierName.trim(),
                description = description.trim(),
                enteredById = actor.id,
                enteredByName = actor.displayName,
                status = if (approved) ExpenseStatus.APPROVED else ExpenseStatus.PENDING,
                approvedById = actor.id.takeIf { approved },
                approvedByName = actor.displayName.takeIf { approved },
                approvedAt = now.takeIf { approved }
            )
        )
    }

    suspend fun approveExpense(expense: ExpenseEntity, actor: UserEntity) = db.withTransaction {
        require(actor.role != UserRole.EMPLOYEE) { "Admin or Super User access is required." }
        require(db.expenseDao().approve(expense.id, actor.id, actor.displayName, System.currentTimeMillis()) == 1) {
            "This expense is no longer pending."
        }
        addAudit("EXPENSE_APPROVED", "EXPENSE", expense.id, actor, "Expense approved")
    }

    suspend fun rejectExpense(expense: ExpenseEntity, actor: UserEntity, reason: String) = db.withTransaction {
        require(actor.role != UserRole.EMPLOYEE) { "Admin or Super User access is required." }
        require(reason.trim().length >= 3) { "Enter a rejection reason." }
        require(db.expenseDao().reject(expense.id, actor.id, actor.displayName, System.currentTimeMillis(), reason.trim()) == 1) {
            "This expense is no longer pending."
        }
        addAudit("EXPENSE_REJECTED", "EXPENSE", expense.id, actor, reason.trim())
    }

    suspend fun cancelExpense(expense: ExpenseEntity, actor: UserEntity, reason: String) = db.withTransaction {
        require(actor.role != UserRole.EMPLOYEE) { "Admin or Super User access is required." }
        require(reason.trim().length >= 3) { "Enter a cancellation reason." }
        require(db.expenseDao().cancel(expense.id, actor.id, actor.displayName, System.currentTimeMillis(), reason.trim()) == 1) {
            "Only an approved expense can be cancelled."
        }
        addAudit("EXPENSE_CANCELLED", "EXPENSE", expense.id, actor, reason.trim())
    }

    suspend fun addInventoryItem(
        actor: UserEntity,
        name: String,
        unit: InventoryUnit,
        minimumStockMilli: Long,
        openingStockMilli: Long
    ) = db.withTransaction {
        require(actor.role != UserRole.EMPLOYEE) { "Admin or Super User access is required." }
        require(name.trim().isNotBlank()) { "Inventory item name is required." }
        require(minimumStockMilli >= 0 && openingStockMilli >= 0) { "Stock quantities cannot be negative." }
        val item = InventoryItemEntity(
            id = UUID.randomUUID().toString(), name = name.trim(), unit = unit,
            minimumStockMilli = minimumStockMilli
        )
        db.inventoryDao().insertItem(item)
        if (openingStockMilli > 0) db.inventoryDao().insertTransaction(
            StockTransactionEntity(
                id = UUID.randomUUID().toString(), inventoryItemId = item.id,
                type = StockTransactionType.OPENING, quantityDeltaMilli = openingStockMilli,
                description = "Opening stock", actorId = actor.id, actorName = actor.displayName
            )
        )
    }

    suspend fun purchaseStock(
        actor: UserEntity,
        item: InventoryItemEntity,
        quantityMilli: Long,
        totalCostPaise: Long,
        paymentMethod: PaymentMethod,
        supplierName: String,
        description: String
    ) = db.withTransaction {
        require(actor.role != UserRole.EMPLOYEE) { "Admin or Super User access is required." }
        require(quantityMilli > 0) { "Purchase quantity must be greater than zero." }
        require(totalCostPaise > 0) { "Purchase cost must be greater than zero." }
        val now = System.currentTimeMillis()
        val transactionId = UUID.randomUUID().toString()
        val expenseId = UUID.randomUUID().toString()
        val currentStock = db.inventoryDao().currentStock(item.id).coerceAtLeast(0)
        val newStock = currentStock + quantityMilli
        val oldValue = currentStock * item.averageCostPaisePerUnit / 1_000
        val newAverage = (oldValue + totalCostPaise) * 1_000 / newStock
        db.inventoryDao().updateItem(item.copy(averageCostPaisePerUnit = newAverage, updatedAt = now))
        db.expenseDao().insert(
            ExpenseEntity(
                id = expenseId, category = "Inventory purchase", amountPaise = totalCostPaise,
                occurredAt = now, paymentMethod = paymentMethod, supplierName = supplierName.trim(),
                description = description.trim().ifBlank { "${item.name} stock purchase" },
                enteredById = actor.id, enteredByName = actor.displayName,
                status = ExpenseStatus.APPROVED, approvedById = actor.id,
                approvedByName = actor.displayName, approvedAt = now,
                linkedStockTransactionId = transactionId, createdAt = now
            )
        )
        db.inventoryDao().insertTransaction(
            StockTransactionEntity(
                id = transactionId, inventoryItemId = item.id, type = StockTransactionType.PURCHASE,
                quantityDeltaMilli = quantityMilli, totalCostPaise = totalCostPaise,
                supplierName = supplierName.trim(), description = description.trim(), expenseId = expenseId,
                actorId = actor.id, actorName = actor.displayName, createdAt = now
            )
        )
    }

    suspend fun adjustStock(
        actor: UserEntity,
        item: InventoryItemEntity,
        type: StockTransactionType,
        quantityDeltaMilli: Long,
        description: String
    ) = db.withTransaction {
        require(actor.role != UserRole.EMPLOYEE) { "Admin or Super User access is required." }
        require(type in setOf(StockTransactionType.ADJUSTMENT, StockTransactionType.WASTAGE, StockTransactionType.SUPPLIER_RETURN)) {
            "Unsupported stock adjustment."
        }
        require(quantityDeltaMilli != 0) { "Enter a stock quantity." }
        require(description.trim().length >= 3) { "Enter a reason." }
        val safeDelta = when (type) {
            StockTransactionType.WASTAGE, StockTransactionType.SUPPLIER_RETURN -> -kotlin.math.abs(quantityDeltaMilli)
            else -> quantityDeltaMilli
        }
        db.inventoryDao().insertTransaction(
            StockTransactionEntity(
                id = UUID.randomUUID().toString(), inventoryItemId = item.id, type = type,
                quantityDeltaMilli = safeDelta, description = description.trim(),
                actorId = actor.id, actorName = actor.displayName
            )
        )
    }

    suspend fun saveRecipeIngredient(
        actor: UserEntity,
        productId: String,
        inventoryItemId: String,
        quantityMilliPerSaleUnit: Long
    ) {
        require(actor.role != UserRole.EMPLOYEE) { "Admin or Super User access is required." }
        require(quantityMilliPerSaleUnit > 0) { "Recipe quantity must be greater than zero." }
        db.inventoryDao().saveRecipeIngredient(
            RecipeIngredientEntity(productId, inventoryItemId, quantityMilliPerSaleUnit)
        )
    }

    suspend fun deleteRecipeIngredient(actor: UserEntity, productId: String, inventoryItemId: String) {
        require(actor.role != UserRole.EMPLOYEE) { "Admin or Super User access is required." }
        db.inventoryDao().deleteRecipeIngredient(productId, inventoryItemId)
    }

    private suspend fun addAudit(
        action: String,
        entityType: String,
        entityId: String,
        actor: UserEntity,
        reason: String
    ) = db.auditDao().insert(
        AuditLogEntity(
            id = UUID.randomUUID().toString(), action = action, entityType = entityType,
            entityId = entityId, actorId = actor.id, actorName = actor.displayName, reason = reason
        )
    )

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

    private suspend fun ensureCategories() {
        val productCategories = db.productDao().categoryNames()
        val orderedNames = buildList {
            add(BillingCategories.MISC)
            addAll(productCategories.filterNot { it == BillingCategories.MISC }.sorted())
        }
        db.categoryDao().insertAll(
            orderedNames.mapIndexed { index, name -> CategoryEntity(name = name, sortOrder = index) }
        )
    }

    private suspend fun ensureCategory(name: String) {
        db.categoryDao().insert(
            CategoryEntity(name = name, sortOrder = db.categoryDao().maxSortOrder() + 1)
        )
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
