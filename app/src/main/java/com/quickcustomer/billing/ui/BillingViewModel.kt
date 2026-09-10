package com.quickcustomer.billing.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickcustomer.billing.QuickCustomerApplication
import com.quickcustomer.billing.data.BillDetails
import com.quickcustomer.billing.data.BillingCategories
import com.quickcustomer.billing.data.CartLine
import com.quickcustomer.billing.data.CategoryEntity
import com.quickcustomer.billing.data.PaymentMethod
import com.quickcustomer.billing.data.ExpenseEntity
import com.quickcustomer.billing.data.InventoryItemEntity
import com.quickcustomer.billing.data.InventoryStock
import com.quickcustomer.billing.data.InventoryUnit
import com.quickcustomer.billing.data.ProductProfitSummary
import com.quickcustomer.billing.data.RecipeIngredientDetail
import com.quickcustomer.billing.data.StockTransactionEntity
import com.quickcustomer.billing.data.StockTransactionType
import com.quickcustomer.billing.data.ProductEntity
import com.quickcustomer.billing.data.Receipt
import com.quickcustomer.billing.data.SaleEntity
import com.quickcustomer.billing.data.ShopSettingsEntity
import com.quickcustomer.billing.data.ProductSalesSummary
import com.quickcustomer.billing.data.UserEntity
import com.quickcustomer.billing.data.UserRole
import com.quickcustomer.billing.domain.AccessPolicy
import com.quickcustomer.billing.domain.AppPermission
import com.quickcustomer.billing.printing.BluetoothPrinterManager
import com.quickcustomer.billing.printing.PairedBluetoothPrinter
import com.quickcustomer.billing.printing.PrintableReceipt
import com.quickcustomer.billing.sync.DeviceMode
import com.quickcustomer.billing.sync.DriveConnectOutcome
import com.quickcustomer.billing.sync.DriveSetupStage
import com.quickcustomer.billing.sync.DriveUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppSection(val label: String) {
    BILLING("Billing"),
    SALES("Sales"),
    EXPENSES("Expenses"),
    INVENTORY("Inventory"),
    PRODUCTS("Products"),
    USERS("Users"),
    SETTINGS("Settings")
}

class BillingViewModel(application: Application) : AndroidViewModel(application) {
    private val quickCustomerApplication = application as QuickCustomerApplication
    private val repository = quickCustomerApplication.repository
    private val driveSyncManager = quickCustomerApplication.driveSyncManager
    private val printerManager = BluetoothPrinterManager(application.applicationContext)
    private var googleAccessToken: String? = null
    private var scheduledSyncJob: Job? = null

    val products: StateFlow<List<ProductEntity>> = repository.products.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val categories: StateFlow<List<CategoryEntity>> = repository.categories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val users: StateFlow<List<UserEntity>> = repository.users.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val sales: StateFlow<List<SaleEntity>> = repository.sales.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0
    )
    val settings: StateFlow<ShopSettingsEntity> = repository.settings
        .map { it ?: ShopSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopSettingsEntity())
    val productSales: StateFlow<List<ProductSalesSummary>> = repository.productSales.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val productProfit: StateFlow<List<ProductProfitSummary>> = repository.productProfit.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val expenses: StateFlow<List<ExpenseEntity>> = repository.expenses.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val inventoryStock: StateFlow<List<InventoryStock>> = repository.inventoryStock.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val stockTransactions: StateFlow<List<StockTransactionEntity>> = repository.stockTransactions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val recipeDetails: StateFlow<List<RecipeIngredientDetail>> = repository.recipeDetails.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    fun productSalesInRange(startInclusive: Long, endExclusive: Long): Flow<List<ProductSalesSummary>> =
        repository.productSalesInRange(startInclusive, endExclusive)

    fun productProfitInRange(startInclusive: Long, endExclusive: Long): Flow<List<ProductProfitSummary>> =
        repository.productProfitInRange(startInclusive, endExclusive)

    var currentUser by mutableStateOf<UserEntity?>(null)
        private set
    var currentSection by mutableStateOf(AppSection.BILLING)
        private set
    var selectedCategory by mutableStateOf<String?>(null)
        private set
    var authReady by mutableStateOf(false)
        private set
    var driveUiState by mutableStateOf(
        DriveUiState(
            stage = DriveSetupStage.REQUIRED,
            email = driveSyncManager.preferences.email,
            deviceMode = driveSyncManager.preferences.mode,
            lastSyncAt = driveSyncManager.preferences.lastSyncAt,
            message = if (driveSyncManager.preferences.isLinked) {
                "Reconnect ${driveSyncManager.preferences.email} to synchronize this device."
            } else {
                "Connect the dedicated store Gmail before creating application users."
            }
        )
    )
        private set
    var needsOwnerSetup by mutableStateOf(false)
        private set
    var loginError by mutableStateOf<String?>(null)
        private set
    var ownerSetupError by mutableStateOf<String?>(null)
        private set
    var operationError by mutableStateOf<String?>(null)
        private set
    var printerMessage by mutableStateOf<String?>(null)
        private set
    var pairedPrinters by mutableStateOf<List<PairedBluetoothPrinter>>(emptyList())
        private set
    var lastReceipt by mutableStateOf<Receipt?>(null)
        private set
    var selectedBillDetails by mutableStateOf<BillDetails?>(null)
        private set
    var isLoadingBill by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var isPrinting by mutableStateOf(false)
        private set
    var isSyncing by mutableStateOf(false)
        private set

    val shouldAutoReconnectDrive: Boolean get() = driveSyncManager.preferences.isLinked
    val isMonitorMode: Boolean get() = driveUiState.deviceMode == DeviceMode.MONITOR

    private val quantities = mutableStateMapOf<String, Int>()
    private val miscProducts = mutableStateMapOf<String, ProductEntity>()

    init {
        viewModelScope.launch {
            runCatching { repository.ensureSeeded() }
                .onFailure { operationError = it.message ?: "Could not initialize local data." }
            val hasUsers = runCatching { repository.hasUsers() }.getOrDefault(false)
            needsOwnerSetup = !hasUsers
            // After the first successful store link, never block existing users from
            // opening their local data just because Google Drive is temporarily offline.
            if (hasUsers && driveSyncManager.preferences.isLinked) authReady = true
        }
    }

    fun onDriveAuthorizationStarted() {
        driveUiState = driveUiState.copy(
            stage = if (authReady) driveUiState.stage else DriveSetupStage.AUTHORIZING,
            message = "Waiting for Google Drive authorization…"
        )
    }

    fun onDriveAuthorizationFailed(message: String) {
        driveUiState = driveUiState.copy(
            stage = if (authReady) driveUiState.stage else DriveSetupStage.ERROR,
            message = message.ifBlank { "Google Drive authorization was not completed." }
        )
        if (authReady) operationError = driveUiState.message
    }

    fun connectGoogleDrive(accessToken: String) {
        if (isSyncing) return
        googleAccessToken = accessToken
        isSyncing = true
        val previousStage = driveUiState.stage
        driveUiState = driveUiState.copy(
            stage = if (authReady) driveUiState.stage else DriveSetupStage.CHECKING_DRIVE,
            message = "Checking this Gmail for Quick Customer store data…"
        )
        viewModelScope.launch {
            runCatching { driveSyncManager.connect(accessToken) }
                .onSuccess { outcome ->
                    needsOwnerSetup = !repository.hasUsers()
                    authReady = true
                    when (outcome) {
                        is DriveConnectOutcome.EmptyDrive -> {
                            driveUiState = DriveUiState(
                                stage = DriveSetupStage.DRIVE_EMPTY,
                                email = outcome.connection.email,
                                deviceMode = outcome.connection.mode,
                                message = if (needsOwnerSetup) {
                                    "New store detected. Create the first Super User to finish setup."
                                } else {
                                    "Drive is empty. Sign in as Super User to upload this device as the primary store."
                                }
                            )
                        }
                        is DriveConnectOutcome.ExistingStore -> {
                            driveUiState = DriveUiState(
                                stage = DriveSetupStage.READY,
                                email = outcome.connection.email,
                                deviceMode = outcome.connection.mode,
                                lastSyncAt = outcome.syncedAt,
                                message = if (outcome.connection.mode == DeviceMode.PRIMARY) {
                                    "Primary billing device connected."
                                } else {
                                    "Monitoring device restored from Google Drive."
                                }
                            )
                            if (outcome.connection.mode == DeviceMode.PRIMARY) scheduleDriveUpload()
                        }
                    }
                }
                .onFailure { failure ->
                    driveUiState = driveUiState.copy(
                        stage = if (authReady) previousStage else DriveSetupStage.ERROR,
                        message = failure.message ?: "Google Drive could not be connected."
                    )
                    if (authReady) operationError = driveUiState.message
                }
            isSyncing = false
        }
    }

    fun continueOnThisDeviceOnly() {
        authReady = true
        driveUiState = DriveUiState(
            stage = DriveSetupStage.LOCAL_ONLY,
            message = "Device-only mode. Data is not synchronized with another device."
        )
    }

    fun syncNow() {
        val token = googleAccessToken
        if (token == null) {
            operationError = "Reconnect the store Gmail to refresh Google Drive access."
            return
        }
        performDriveSync(token)
    }

    fun createInitialOwner(username: String, displayName: String, password: String) {
        if (!authReady || !needsOwnerSetup) return
        ownerSetupError = null
        viewModelScope.launch {
            runCatching { repository.createInitialOwner(username, displayName, password) }
                .onSuccess { owner ->
                    currentUser = owner
                    needsOwnerSetup = false
                    initializeDriveStoreIfNeeded()
                }
                .onFailure { ownerSetupError = it.message ?: "Owner account could not be created." }
        }
    }

    fun clearOwnerSetupError() { ownerSetupError = null }

    fun login(username: String, password: String) {
        if (!authReady) return
        loginError = null
        viewModelScope.launch {
            currentUser = repository.authenticate(username, password)
            if (currentUser == null) loginError = "Incorrect username/password or inactive user."
            else {
                if (isMonitorMode) currentSection = AppSection.SALES
                initializeDriveStoreIfNeeded()
            }
        }
    }

    fun clearLoginError() { loginError = null }

    fun logout() {
        currentUser = null
        currentSection = AppSection.BILLING
        quantities.clear()
        miscProducts.clear()
        lastReceipt = null
        selectedBillDetails = null
    }

    fun selectSection(section: AppSection) {
        val role = currentUser?.role ?: return
        if (isMonitorMode && section !in setOf(AppSection.SALES, AppSection.EXPENSES, AppSection.INVENTORY)) {
            operationError = "This is a read-only monitoring device."
            return
        }
        val permitted = when (section) {
            AppSection.BILLING -> AccessPolicy.allows(role, AppPermission.CREATE_BILL)
            AppSection.SALES -> true
            AppSection.EXPENSES -> AccessPolicy.allows(role, AppPermission.VIEW_EXPENSES)
            AppSection.INVENTORY -> AccessPolicy.allows(role, AppPermission.VIEW_INVENTORY)
            AppSection.PRODUCTS -> AccessPolicy.allows(role, AppPermission.MANAGE_PRODUCTS)
            AppSection.USERS -> AccessPolicy.allows(role, AppPermission.MANAGE_USERS)
            AppSection.SETTINGS -> AccessPolicy.allows(role, AppPermission.MANAGE_SHOP)
        }
        if (permitted) currentSection = section else operationError = "Your role does not allow this action."
    }
    fun selectCategory(category: String) { selectedCategory = category }

    fun cartLines(): List<CartLine> = (products.value + miscProducts.values).mapNotNull { product ->
        quantities[product.id]?.takeIf { it > 0 }?.let { CartLine(product, it) }
    }

    fun cartCount(): Int = quantities.values.sum()
    fun cartTotalPaise(): Long = cartLines().sumOf { it.lineTotalPaise }

    fun add(product: ProductEntity) {
        quantities[product.id] = (quantities[product.id] ?: 0) + 1
    }

    fun decrement(product: ProductEntity) {
        val next = (quantities[product.id] ?: 0) - 1
        if (next <= 0) {
            quantities.remove(product.id)
            miscProducts.remove(product.id)
        } else quantities[product.id] = next
    }

    fun addMisc(pricePaise: Long, description: String) {
        if (pricePaise <= 0) {
            operationError = "Enter a Misc price greater than zero."
            return
        }
        val id = "misc-${UUID.randomUUID()}"
        val product = ProductEntity(
            id = id,
            category = BillingCategories.MISC,
            name = description.trim().ifBlank { "Misc item" },
            pricePaise = pricePaise,
            sortOrder = Int.MAX_VALUE
        )
        miscProducts[id] = product
        quantities[id] = 1
    }

    fun clearCart() {
        quantities.clear()
        miscProducts.clear()
    }

    fun checkout(
        paymentMethod: PaymentMethod,
        requestedDiscountPaise: Long,
        cashReceivedPaise: Long? = null
    ) {
        if (isMonitorMode) {
            operationError = "This is a read-only monitoring device."
            return
        }
        val user = currentUser ?: return
        val lines = cartLines()
        if (lines.isEmpty() || isSaving) return
        isSaving = true
        operationError = null
        viewModelScope.launch {
            val permittedDiscount = if (user.role == UserRole.EMPLOYEE) 0 else requestedDiscountPaise
            runCatching {
                repository.completeSale(
                    cashier = user,
                    lines = lines,
                    paymentMethod = paymentMethod,
                    requestedDiscountPaise = permittedDiscount,
                    cashReceivedPaise = cashReceivedPaise,
                    settings = settings.value,
                    businessId = driveSyncManager.preferences.storeId.ifBlank { "business-demo" },
                    shopId = "shop-main",
                    deviceId = driveSyncManager.preferences.deviceId
                )
            }
                .onSuccess {
                    quantities.clear()
                    miscProducts.clear()
                    lastReceipt = it
                    val printerSettings = settings.value
                    if (printerSettings.printerEnabled && printerSettings.printerAutoPrint) {
                        printReceipt(it, showSuccess = false)
                    }
                    scheduleDriveUpload()
                }
                .onFailure { operationError = it.message ?: "The bill could not be saved." }
            isSaving = false
        }
    }

    fun dismissReceipt() { lastReceipt = null }
    fun openBill(sale: SaleEntity) {
        val user = currentUser ?: return
        if (user.role == UserRole.EMPLOYEE && sale.cashierId != user.id) {
            operationError = "Employees can view only their own bills."
            return
        }
        if (isLoadingBill) return
        isLoadingBill = true
        viewModelScope.launch {
            runCatching { repository.billDetails(sale, settings.value) }
                .onSuccess { selectedBillDetails = it }
                .onFailure { operationError = it.message ?: "Bill details could not be opened." }
            isLoadingBill = false
        }
    }
    fun dismissBillDetails() { selectedBillDetails = null }
    fun dismissError() { operationError = null }
    fun dismissPrinterMessage() { printerMessage = null }

    fun refreshPairedPrinters() {
        runCatching { printerManager.pairedPrinters() }
            .onSuccess { pairedPrinters = it }
            .onFailure {
                pairedPrinters = emptyList()
                operationError = it.message ?: "Paired printers could not be loaded."
            }
    }

    fun testPrinter(settings: ShopSettingsEntity) {
        runPrinterJob("Printer test completed.") { printerManager.printTest(settings) }
    }

    fun printReceipt(receipt: Receipt, showSuccess: Boolean = true) {
        val success = if (showSuccess) "Receipt printed successfully." else null
        runPrinterJob(success) {
            printerManager.print(PrintableReceipt.from(receipt), receipt.settings)
        }
    }

    fun printBill(details: BillDetails) {
        runPrinterJob("Receipt printed successfully.") {
            printerManager.print(PrintableReceipt.from(details), settings.value)
        }
    }

    fun saveProduct(existing: ProductEntity?, name: String, category: String, priceRupees: Long) {
        if (!hasPermission(AppPermission.MANAGE_PRODUCTS)) return
        viewModelScope.launch {
            runCatching {
                if (existing == null) repository.addProduct(name, category, priceRupees)
                else repository.updateProduct(
                    existing.copy(name = name.trim(), category = category.trim(), pricePaise = priceRupees * 100)
                )
            }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "Product could not be saved." }
        }
    }

    fun toggleProduct(product: ProductEntity) {
        if (!hasPermission(AppPermission.MANAGE_PRODUCTS)) return
        viewModelScope.launch {
            repository.updateProduct(product.copy(isActive = !product.isActive))
            scheduleDriveUpload()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        if (!hasPermission(AppPermission.MANAGE_PRODUCTS)) return
        quantities.remove(product.id)
        viewModelScope.launch {
            runCatching { repository.deleteProduct(product) }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "Product could not be removed." }
        }
    }

    fun saveCategoryOrder(names: List<String>) {
        if (!hasPermission(AppPermission.MANAGE_PRODUCTS)) return
        viewModelScope.launch {
            runCatching { repository.saveCategoryOrder(names) }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "Category order could not be saved." }
        }
    }

    fun addUser(username: String, displayName: String, role: UserRole, password: String) {
        if (!hasPermission(AppPermission.MANAGE_USERS)) return
        viewModelScope.launch {
            runCatching { repository.addUser(username, displayName, role, password) }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "User could not be created." }
        }
    }

    fun toggleUser(user: UserEntity) {
        if (!hasPermission(AppPermission.MANAGE_USERS)) return
        if (user.id == currentUser?.id) {
            operationError = "You cannot deactivate your own signed-in account."
            return
        }
        viewModelScope.launch {
            repository.updateUser(user.copy(isActive = !user.isActive))
            scheduleDriveUpload()
        }
    }

    fun cancelSale(sale: SaleEntity, reason: String) {
        if (!hasPermission(AppPermission.CANCEL_COMPLETED_BILL)) return
        val actor = currentUser ?: return
        viewModelScope.launch {
            runCatching { repository.cancelSale(sale, actor, reason) }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "Bill could not be cancelled." }
        }
    }

    fun saveSettings(settings: ShopSettingsEntity) {
        if (!hasPermission(AppPermission.MANAGE_SHOP)) return
        val actor = currentUser ?: return
        viewModelScope.launch {
            runCatching { repository.saveSettings(settings, actor) }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "Settings could not be saved." }
        }
    }

    fun addExpense(
        category: String,
        amountPaise: Long,
        paymentMethod: PaymentMethod,
        supplierName: String,
        description: String,
        occurredAt: Long
    ) {
        if (!hasPermission(AppPermission.ADD_EXPENSE)) return
        val actor = currentUser ?: return
        viewModelScope.launch {
            runCatching {
                repository.addExpense(actor, category, amountPaise, paymentMethod, supplierName, description, occurredAt)
            }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "Expense could not be saved." }
        }
    }

    fun approveExpense(expense: ExpenseEntity) = expenseAction(AppPermission.APPROVE_EXPENSES) { actor ->
        repository.approveExpense(expense, actor)
    }

    fun rejectExpense(expense: ExpenseEntity, reason: String) = expenseAction(AppPermission.APPROVE_EXPENSES) { actor ->
        repository.rejectExpense(expense, actor, reason)
    }

    fun cancelExpense(expense: ExpenseEntity, reason: String) = expenseAction(AppPermission.APPROVE_EXPENSES) { actor ->
        repository.cancelExpense(expense, actor, reason)
    }

    private fun expenseAction(permission: AppPermission, action: suspend (UserEntity) -> Unit) {
        if (!hasPermission(permission)) return
        val actor = currentUser ?: return
        viewModelScope.launch {
            runCatching { action(actor) }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "Expense could not be updated." }
        }
    }

    fun addInventoryItem(name: String, unit: InventoryUnit, minimumMilli: Long, openingMilli: Long) {
        if (!hasPermission(AppPermission.MANAGE_INVENTORY)) return
        val actor = currentUser ?: return
        viewModelScope.launch {
            runCatching { repository.addInventoryItem(actor, name, unit, minimumMilli, openingMilli) }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "Inventory item could not be saved." }
        }
    }

    fun purchaseStock(
        item: InventoryItemEntity,
        quantityMilli: Long,
        totalCostPaise: Long,
        paymentMethod: PaymentMethod,
        supplierName: String,
        description: String
    ) {
        if (!hasPermission(AppPermission.MANAGE_INVENTORY)) return
        val actor = currentUser ?: return
        viewModelScope.launch {
            runCatching { repository.purchaseStock(actor, item, quantityMilli, totalCostPaise, paymentMethod, supplierName, description) }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "Stock purchase could not be saved." }
        }
    }

    fun adjustStock(item: InventoryItemEntity, type: StockTransactionType, quantityMilli: Long, description: String) {
        if (!hasPermission(AppPermission.MANAGE_INVENTORY)) return
        val actor = currentUser ?: return
        viewModelScope.launch {
            runCatching { repository.adjustStock(actor, item, type, quantityMilli, description) }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "Stock adjustment could not be saved." }
        }
    }

    fun saveRecipeIngredient(productId: String, inventoryItemId: String, quantityMilli: Long) {
        if (!hasPermission(AppPermission.MANAGE_INVENTORY)) return
        val actor = currentUser ?: return
        viewModelScope.launch {
            runCatching { repository.saveRecipeIngredient(actor, productId, inventoryItemId, quantityMilli) }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "Recipe could not be saved." }
        }
    }

    fun deleteRecipeIngredient(productId: String, inventoryItemId: String) {
        if (!hasPermission(AppPermission.MANAGE_INVENTORY)) return
        val actor = currentUser ?: return
        viewModelScope.launch {
            runCatching { repository.deleteRecipeIngredient(actor, productId, inventoryItemId) }
                .onSuccess { scheduleDriveUpload() }
                .onFailure { operationError = it.message ?: "Recipe ingredient could not be removed." }
        }
    }

    private fun runPrinterJob(successMessage: String?, action: suspend () -> Unit) {
        if (isPrinting) {
            operationError = "A print job is already running."
            return
        }
        isPrinting = true
        operationError = null
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { if (successMessage != null) printerMessage = successMessage }
                .onFailure { failure ->
                    operationError = failure.message ?: "The receipt could not be printed."
                }
            isPrinting = false
        }
    }

    private fun hasPermission(permission: AppPermission): Boolean {
        if (isMonitorMode) {
            operationError = "This is a read-only monitoring device."
            return false
        }
        val role = currentUser?.role
        if (role != null && AccessPolicy.allows(role, permission)) return true
        operationError = "Your role does not allow this action."
        return false
    }

    private fun initializeDriveStoreIfNeeded() {
        if (driveUiState.stage != DriveSetupStage.DRIVE_EMPTY) return
        val user = currentUser ?: return
        if (user.role != UserRole.SUPER_USER) {
            operationError = "The Super User must sign in once to initialize Google Drive."
            return
        }
        val token = googleAccessToken ?: return
        isSyncing = true
        driveUiState = driveUiState.copy(message = "Creating protected store synchronization data…")
        viewModelScope.launch {
            runCatching { driveSyncManager.initializePrimaryStore(token) }
                .onSuccess { result ->
                    driveUiState = driveUiState.copy(
                        stage = DriveSetupStage.READY,
                        deviceMode = DeviceMode.PRIMARY,
                        lastSyncAt = result.syncedAt,
                        message = "Primary billing device synchronized."
                    )
                }
                .onFailure { failure ->
                    driveUiState = driveUiState.copy(
                        message = failure.message ?: "The store could not be initialized in Google Drive."
                    )
                    operationError = driveUiState.message
                }
            isSyncing = false
        }
    }

    private fun scheduleDriveUpload() {
        if (driveUiState.deviceMode != DeviceMode.PRIMARY || driveUiState.stage != DriveSetupStage.READY) return
        val token = googleAccessToken ?: return
        scheduledSyncJob?.cancel()
        scheduledSyncJob = viewModelScope.launch {
            delay(1_500)
            performDriveSync(token)
        }
    }

    private fun performDriveSync(token: String) {
        if (isSyncing) return
        isSyncing = true
        val previousStage = driveUiState.stage
        driveUiState = driveUiState.copy(message = "Synchronizing with Google Drive…")
        viewModelScope.launch {
            runCatching { driveSyncManager.sync(token) }
                .onSuccess { result ->
                    driveUiState = driveUiState.copy(
                        stage = DriveSetupStage.READY,
                        lastSyncAt = result.syncedAt,
                        message = if (result.uploaded) {
                            "Store data uploaded successfully."
                        } else {
                            "Latest store activity downloaded."
                        }
                    )
                }
                .onFailure { failure ->
                    driveUiState = driveUiState.copy(
                        stage = if (authReady) previousStage else DriveSetupStage.ERROR,
                        message = failure.message ?: "Google Drive synchronization failed."
                    )
                    operationError = driveUiState.message
                }
            isSyncing = false
        }
    }
}
