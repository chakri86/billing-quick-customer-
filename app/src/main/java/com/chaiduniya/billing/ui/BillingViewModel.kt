package com.chaiduniya.billing.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chaiduniya.billing.ChaiDuniyaApplication
import com.chaiduniya.billing.data.CartLine
import com.chaiduniya.billing.data.PaymentMethod
import com.chaiduniya.billing.data.ProductEntity
import com.chaiduniya.billing.data.Receipt
import com.chaiduniya.billing.data.SaleEntity
import com.chaiduniya.billing.data.UserEntity
import com.chaiduniya.billing.data.UserRole
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppSection(val label: String) { BILLING("Billing"), SALES("Sales"), PRODUCTS("Products"), USERS("Users") }

class BillingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ChaiDuniyaApplication).repository

    val products: StateFlow<List<ProductEntity>> = repository.products.stateIn(
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

    var currentUser by mutableStateOf<UserEntity?>(null)
        private set
    var currentSection by mutableStateOf(AppSection.BILLING)
        private set
    var selectedCategory by mutableStateOf<String?>(null)
        private set
    var authReady by mutableStateOf(false)
        private set
    var loginError by mutableStateOf<String?>(null)
        private set
    var operationError by mutableStateOf<String?>(null)
        private set
    var lastReceipt by mutableStateOf<Receipt?>(null)
        private set
    var isSaving by mutableStateOf(false)
        private set

    private val quantities = mutableStateMapOf<String, Int>()

    init {
        viewModelScope.launch {
            runCatching { repository.ensureSeeded() }
                .onFailure { operationError = it.message ?: "Could not initialize local data." }
            authReady = true
        }
    }

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
        lastReceipt = null
    }

    fun selectSection(section: AppSection) { currentSection = section }
    fun selectCategory(category: String) { selectedCategory = category }

    fun cartLines(): List<CartLine> = products.value.mapNotNull { product ->
        quantities[product.id]?.takeIf { it > 0 }?.let { CartLine(product, it) }
    }

    fun cartCount(): Int = quantities.values.sum()
    fun cartTotalPaise(): Long = cartLines().sumOf { it.lineTotalPaise }

    fun add(product: ProductEntity) {
        quantities[product.id] = (quantities[product.id] ?: 0) + 1
    }

    fun decrement(product: ProductEntity) {
        val next = (quantities[product.id] ?: 0) - 1
        if (next <= 0) quantities.remove(product.id) else quantities[product.id] = next
    }

    fun clearCart() = quantities.clear()

    fun checkout(paymentMethod: PaymentMethod) {
        val user = currentUser ?: return
        val lines = cartLines()
        if (lines.isEmpty() || isSaving) return
        isSaving = true
        operationError = null
        viewModelScope.launch {
            runCatching { repository.completeSale(user, lines, paymentMethod) }
                .onSuccess {
                    quantities.clear()
                    lastReceipt = it
                }
                .onFailure { operationError = it.message ?: "The bill could not be saved." }
            isSaving = false
        }
    }

    fun dismissReceipt() { lastReceipt = null }
    fun dismissError() { operationError = null }

    fun saveProduct(existing: ProductEntity?, name: String, category: String, priceRupees: Long) {
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
        viewModelScope.launch { repository.updateProduct(product.copy(isActive = !product.isActive)) }
    }

    fun addUser(username: String, displayName: String, role: UserRole, password: String) {
        viewModelScope.launch {
            runCatching { repository.addUser(username, displayName, role, password) }
                .onFailure { operationError = it.message ?: "User could not be created." }
        }
    }

    fun toggleUser(user: UserEntity) {
        if (user.id == currentUser?.id) {
            operationError = "You cannot deactivate your own signed-in account."
            return
        }
        viewModelScope.launch { repository.updateUser(user.copy(isActive = !user.isActive)) }
    }
}
