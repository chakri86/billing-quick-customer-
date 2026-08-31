package com.chaiduniya.billing.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chaiduniya.billing.ChaiDuniyaApplication
import com.chaiduniya.billing.data.BillDetails
import com.chaiduniya.billing.data.BillingCategories
import com.chaiduniya.billing.data.CartLine
import com.chaiduniya.billing.data.CategoryEntity
import com.chaiduniya.billing.data.PaymentMethod
import com.chaiduniya.billing.data.ProductEntity
import com.chaiduniya.billing.data.Receipt
import com.chaiduniya.billing.data.SaleEntity
import com.chaiduniya.billing.data.ShopSettingsEntity
import com.chaiduniya.billing.data.ProductSalesSummary
import com.chaiduniya.billing.data.UserEntity
import com.chaiduniya.billing.data.UserRole
import com.chaiduniya.billing.domain.AccessPolicy
import com.chaiduniya.billing.domain.AppPermission
import com.chaiduniya.billing.printing.BluetoothPrinterManager
import com.chaiduniya.billing.printing.PairedBluetoothPrinter
import com.chaiduniya.billing.printing.PrintableReceipt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppSection(val label: String) {
    BILLING("Billing"),
    SALES("Sales"),
    PRODUCTS("Products"),
    USERS("Users"),
    SETTINGS("Settings")
}

class BillingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ChaiDuniyaApplication).repository
    private val printerManager = BluetoothPrinterManager(application.applicationContext)

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

    var currentUser by mutableStateOf<UserEntity?>(null)
        private set
    var currentSection by mutableStateOf(AppSection.BILLING)
        private set
    var selectedCategory by mutableStateOf<String?>(null)
        private set
    var authReady by mutableStateOf(false)
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

    private val quantities = mutableStateMapOf<String, Int>()
    private val miscProducts = mutableStateMapOf<String, ProductEntity>()

    init {
        viewModelScope.launch {
            runCatching { repository.ensureSeeded() }
                .onFailure { operationError = it.message ?: "Could not initialize local data." }
            needsOwnerSetup = runCatching { !repository.hasUsers() }.getOrDefault(false)
            authReady = true
        }
    }

    fun createInitialOwner(username: String, displayName: String, password: String) {
        if (!authReady || !needsOwnerSetup) return
        ownerSetupError = null
        viewModelScope.launch {
            runCatching { repository.createInitialOwner(username, displayName, password) }
                .onSuccess { owner ->
                    currentUser = owner
                    needsOwnerSetup = false
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
        val permitted = when (section) {
            AppSection.BILLING -> AccessPolicy.allows(role, AppPermission.CREATE_BILL)
            AppSection.SALES -> true
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
        val user = currentUser ?: return
        val lines = cartLines()
        if (lines.isEmpty() || isSaving) return
        isSaving = true
        operationError = null
        viewModelScope.launch {
            val permittedDiscount = if (user.role == UserRole.EMPLOYEE) 0 else requestedDiscountPaise
            runCatching {
                repository.completeSale(
                    user,
                    lines,
                    paymentMethod,
                    permittedDiscount,
                    cashReceivedPaise,
                    settings.value
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
            }.onFailure { operationError = it.message ?: "Product could not be saved." }
        }
    }

    fun toggleProduct(product: ProductEntity) {
        if (!hasPermission(AppPermission.MANAGE_PRODUCTS)) return
        viewModelScope.launch { repository.updateProduct(product.copy(isActive = !product.isActive)) }
    }

    fun deleteProduct(product: ProductEntity) {
        if (!hasPermission(AppPermission.MANAGE_PRODUCTS)) return
        quantities.remove(product.id)
        viewModelScope.launch {
            runCatching { repository.deleteProduct(product) }
                .onFailure { operationError = it.message ?: "Product could not be removed." }
        }
    }

    fun saveCategoryOrder(names: List<String>) {
        if (!hasPermission(AppPermission.MANAGE_PRODUCTS)) return
        viewModelScope.launch {
            runCatching { repository.saveCategoryOrder(names) }
                .onFailure { operationError = it.message ?: "Category order could not be saved." }
        }
    }

    fun addUser(username: String, displayName: String, role: UserRole, password: String) {
        if (!hasPermission(AppPermission.MANAGE_USERS)) return
        viewModelScope.launch {
            runCatching { repository.addUser(username, displayName, role, password) }
                .onFailure { operationError = it.message ?: "User could not be created." }
        }
    }

    fun toggleUser(user: UserEntity) {
        if (!hasPermission(AppPermission.MANAGE_USERS)) return
        if (user.id == currentUser?.id) {
            operationError = "You cannot deactivate your own signed-in account."
            return
        }
        viewModelScope.launch { repository.updateUser(user.copy(isActive = !user.isActive)) }
    }

    fun cancelSale(sale: SaleEntity, reason: String) {
        if (!hasPermission(AppPermission.CANCEL_COMPLETED_BILL)) return
        val actor = currentUser ?: return
        viewModelScope.launch {
            runCatching { repository.cancelSale(sale, actor, reason) }
                .onFailure { operationError = it.message ?: "Bill could not be cancelled." }
        }
    }

    fun saveSettings(settings: ShopSettingsEntity) {
        if (!hasPermission(AppPermission.MANAGE_SHOP)) return
        val actor = currentUser ?: return
        viewModelScope.launch {
            runCatching { repository.saveSettings(settings, actor) }
                .onFailure { operationError = it.message ?: "Settings could not be saved." }
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
        val role = currentUser?.role
        if (role != null && AccessPolicy.allows(role, permission)) return true
        operationError = "Your role does not allow this action."
        return false
    }
}
