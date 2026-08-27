@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chaiduniya.billing.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaiduniya.billing.data.CartLine
import com.chaiduniya.billing.data.PaymentMethod
import com.chaiduniya.billing.data.ProductEntity
import com.chaiduniya.billing.data.Receipt
import com.chaiduniya.billing.data.SaleEntity
import com.chaiduniya.billing.data.UserEntity
import com.chaiduniya.billing.data.UserRole
import com.chaiduniya.billing.domain.Money
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillingApp(viewModel: BillingViewModel) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val user = viewModel.currentUser
        if (user == null) LoginScreen(viewModel) else AppShell(viewModel, user)
    }

    viewModel.operationError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } },
            title = { Text("Action not completed") },
            text = { Text(message) }
        )
    }
    viewModel.lastReceipt?.let { ReceiptDialog(it, viewModel::dismissReceipt) }
}

@Composable
private fun LoginScreen(viewModel: BillingViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp).widthIn(max = 480.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.Storefront, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Chai Duniya", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Billing Console", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; viewModel.clearLoginError() },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; viewModel.clearLoginError() },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focus.clearFocus(); viewModel.login(username, password)
                    })
                )
                viewModel.loginError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = { focus.clearFocus(); viewModel.login(username, password) },
                    enabled = viewModel.authReady && username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (!viewModel.authReady) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Sign in")
                }
                Text(
                    "Demo: owner / Owner@123  •  admin / Admin@123  •  cashier / Cashier@123",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class SectionItem(val section: AppSection, val icon: ImageVector)

private fun sectionsFor(role: UserRole): List<SectionItem> = buildList {
    add(SectionItem(AppSection.BILLING, Icons.Default.PointOfSale))
    add(SectionItem(AppSection.SALES, Icons.Default.BarChart))
    if (role != UserRole.EMPLOYEE) add(SectionItem(AppSection.PRODUCTS, Icons.Default.Inventory2))
    if (role == UserRole.SUPER_USER) add(SectionItem(AppSection.USERS, Icons.Default.AdminPanelSettings))
}

@Composable
private fun AppShell(viewModel: BillingViewModel, user: UserEntity) {
    val sections = remember(user.role) { sectionsFor(user.role) }
    val pending by viewModel.pendingSyncCount.collectAsState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(viewModel.currentSection.label, fontWeight = FontWeight.SemiBold)
                            Text("${user.displayName} • ${user.role.displayName()}", fontSize = 12.sp)
                        }
                    },
                    actions = {
                        AssistChip(
                            onClick = {},
                            label = { Text(if (pending == 0) "Local data ready" else "$pending pending") },
                            leadingIcon = { Icon(Icons.Default.Sync, null, Modifier.size(18.dp)) }
                        )
                        IconButton(onClick = viewModel::logout) { Icon(Icons.AutoMirrored.Filled.Logout, "Sign out") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        sections.forEach { item ->
                            NavigationBarItem(
                                selected = viewModel.currentSection == item.section,
                                onClick = { viewModel.selectSection(item.section) },
                                icon = { Icon(item.icon, null) },
                                label = { Text(item.section.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (useRail) {
                    NavigationRail {
                        Spacer(Modifier.height(8.dp))
                        sections.forEach { item ->
                            NavigationRailItem(
                                selected = viewModel.currentSection == item.section,
                                onClick = { viewModel.selectSection(item.section) },
                                icon = { Icon(item.icon, null) },
                                label = { Text(item.section.label) }
                            )
                        }
                    }
                    Divider(Modifier.fillMaxHeight().width(1.dp))
                }
                when (viewModel.currentSection) {
                    AppSection.BILLING -> BillingScreen(viewModel)
                    AppSection.SALES -> SalesScreen(viewModel, user)
                    AppSection.PRODUCTS -> ProductsScreen(viewModel)
                    AppSection.USERS -> UsersScreen(viewModel)
                }
            }
        }
    }
}

@Composable
private fun BillingScreen(viewModel: BillingViewModel) {
    val allProducts by viewModel.products.collectAsState()
    val active = allProducts.filter { it.isActive }
    val categories = active.map { it.category }.distinct()
    LaunchedEffect(categories) {
        if (viewModel.selectedCategory !in categories) categories.firstOrNull()?.let(viewModel::selectCategory)
    }
    val visible = active.filter { it.category == viewModel.selectedCategory }
    var cartSheet by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        when {
            maxWidth >= 840.dp -> Row(Modifier.fillMaxSize()) {
                CategoryColumn(categories, viewModel.selectedCategory, viewModel::selectCategory, Modifier.width(160.dp))
                Divider(Modifier.fillMaxHeight().width(1.dp))
                ProductPanel(visible, viewModel::add, Modifier.weight(1f))
                Divider(Modifier.fillMaxHeight().width(1.dp))
                CartPane(viewModel, Modifier.width(340.dp))
            }
            maxWidth >= 600.dp -> Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f)) {
                    CategoryStrip(categories, viewModel.selectedCategory, viewModel::selectCategory)
                    ProductPanel(visible, viewModel::add, Modifier.weight(1f))
                }
                Divider(Modifier.fillMaxHeight().width(1.dp))
                CartPane(viewModel, Modifier.width(330.dp))
            }
            else -> Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    CategoryStrip(categories, viewModel.selectedCategory, viewModel::selectCategory)
                    ProductPanel(visible, viewModel::add, Modifier.weight(1f))
                }
                ExtendedFloatingActionButton(
                    onClick = { cartSheet = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
                    icon = { Icon(Icons.Default.ShoppingCart, null) },
                    text = { Text("Cart (${viewModel.cartCount()})") }
                )
            }
        }
    }

    if (cartSheet) {
        ModalBottomSheet(onDismissRequest = { cartSheet = false }) {
            CartPane(viewModel, Modifier.fillMaxWidth().heightIn(min = 360.dp, max = 620.dp))
        }
    }
}

@Composable
private fun CategoryColumn(
    categories: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Categories", fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp)) }
        items(categories) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(category) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryStrip(categories: List<String>, selected: String?, onSelect: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(selected = category == selected, onClick = { onSelect(category) }, label = { Text(category) })
        }
    }
}

@Composable
private fun ProductPanel(products: List<ProductEntity>, onAdd: (ProductEntity) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(12.dp)) {
        Text("Products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        if (products.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No active products in this category") }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(128.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    Card(
                        modifier = Modifier.height(112.dp).clickable { onAdd(product) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text(Money.format(product.pricePaise), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartPane(viewModel: BillingViewModel, modifier: Modifier = Modifier) {
    val lines = viewModel.cartLines()
    var checkoutDialog by remember { mutableStateOf(false) }
    Column(modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Current bill", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (lines.isNotEmpty()) IconButton(onClick = viewModel::clearCart) { Icon(Icons.Default.DeleteSweep, "Clear cart") }
        }
        Divider()
        if (lines.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("Tap a product to start", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(lines, key = { it.product.id }) { line ->
                    CartLineRow(line, { viewModel.decrement(line.product) }, { viewModel.add(line.product) })
                }
            }
        }
        Divider()
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Total", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Text(Money.format(viewModel.cartTotalPaise()), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { checkoutDialog = true },
            enabled = lines.isNotEmpty() && !viewModel.isSaving,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(if (viewModel.isSaving) "Saving…" else "Proceed to payment") }
    }
    if (checkoutDialog) CheckoutDialog(
        totalPaise = viewModel.cartTotalPaise(),
        onDismiss = { checkoutDialog = false },
        onConfirm = { method -> checkoutDialog = false; viewModel.checkout(method) }
    )
}

@Composable
private fun CartLineRow(line: CartLine, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(line.product.name, fontWeight = FontWeight.Medium)
                Text("${Money.format(line.product.pricePaise)} each", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onMinus) { Icon(Icons.Default.Remove, "Decrease") }
            Text(line.quantity.toString(), fontWeight = FontWeight.Bold)
            IconButton(onClick = onPlus) { Icon(Icons.Default.Add, "Increase") }
            Text(Money.format(line.lineTotalPaise), modifier = Modifier.width(78.dp), fontWeight = FontWeight.SemiBold)
        }
        Divider()
    }
}

@Composable
private fun CheckoutDialog(totalPaise: Long, onDismiss: () -> Unit, onConfirm: (PaymentMethod) -> Unit) {
    var selected by remember { mutableStateOf(PaymentMethod.CASH) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment") },
        text = {
            Column {
                Text("Amount: ${Money.format(totalPaise)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                PaymentMethod.entries.forEach { method ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = method }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == method, onClick = { selected = method })
                        Text(method.name)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(selected) }) { Text("Confirm payment") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ReceiptDialog(receipt: Receipt, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment successful") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(receipt.sale.invoiceNumber, fontWeight = FontWeight.Bold)
                Text(formatDate(receipt.sale.createdAt), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                receipt.lines.forEach { line ->
                    Row(Modifier.fillMaxWidth()) {
                        Text("${line.quantity} × ${line.product.name}", Modifier.weight(1f))
                        Text(Money.format(line.lineTotalPaise))
                    }
                }
                Divider(Modifier.padding(vertical = 10.dp))
                Row {
                    Text("Total", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(Money.format(receipt.sale.totalPaise), fontWeight = FontWeight.Bold)
                }
                Text("Paid by ${receipt.sale.paymentMethod.name}")
                Text("Saved locally • waiting for cloud sync", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("New bill") } }
    )
}

@Composable
private fun SalesScreen(viewModel: BillingViewModel, user: UserEntity) {
    val allSales by viewModel.sales.collectAsState()
    val sales = if (user.role == UserRole.EMPLOYEE) allSales.filter { it.cashierId == user.id } else allSales
    val total = sales.filterNot { it.isCancelled }.sumOf { it.totalPaise }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(if (user.role == UserRole.EMPLOYEE) "My sales" else "Shop sales", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Bills", sales.size.toString(), Modifier.weight(1f))
            SummaryCard("Sales", Money.format(total), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        if (sales.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No completed bills yet") }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sales, key = { it.id }) { sale -> SaleRow(sale) }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SaleRow(sale: SaleEntity) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(sale.invoiceNumber, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text("${formatDate(sale.createdAt)} • ${sale.cashierName} • ${sale.paymentMethod.name}") },
            trailingContent = { Text(Money.format(sale.totalPaise), fontWeight = FontWeight.Bold) }
        )
    }
}

@Composable
private fun ProductsScreen(viewModel: BillingViewModel) {
    val products by viewModel.products.collectAsState()
    var editing by remember { mutableStateOf<ProductEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(260.dp),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products, key = { it.id }) { product ->
                OutlinedCard(
                    modifier = Modifier.clickable { editing = product },
                    border = BorderStroke(1.dp, if (product.isActive) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(product.category, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Money.format(product.pricePaise), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(if (product.isActive) "Active" else "Disabled", style = MaterialTheme.typography.bodySmall)
                            Switch(checked = product.isActive, onCheckedChange = { viewModel.toggleProduct(product) })
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { creating = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Default.Add, "Add product")
        }
    }
    if (creating) ProductDialog(null, { creating = false }) { name, category, price ->
        viewModel.saveProduct(null, name, category, price); creating = false
    }
    editing?.let { product ->
        ProductDialog(product, { editing = null }) { name, category, price ->
            viewModel.saveProduct(product, name, category, price); editing = null
        }
    }
}

@Composable
private fun ProductDialog(product: ProductEntity?, onDismiss: () -> Unit, onSave: (String, String, Long) -> Unit) {
    var name by remember(product) { mutableStateOf(product?.name.orEmpty()) }
    var category by remember(product) { mutableStateOf(product?.category.orEmpty()) }
    var price by remember(product) { mutableStateOf(product?.pricePaise?.div(100)?.toString().orEmpty()) }
    val valid = name.isNotBlank() && category.isNotBlank() && (price.toLongOrNull() ?: 0) > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Add product" else "Edit product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Product name") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true)
                OutlinedTextField(
                    price, { price = it.filter(Char::isDigit) }, label = { Text("Price in ₹") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (product != null) Text("Changing the price affects future bills only.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { onSave(name, category, price.toLong()) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun UsersScreen(viewModel: BillingViewModel) {
    val users by viewModel.users.collectAsState()
    var creating by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("User access", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Only the Super User can create accounts and change access.")
                Spacer(Modifier.height(8.dp))
            }
            items(users, key = { it.id }) { user ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(user.displayName, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("@${user.username} • ${user.role.displayName()}") },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (user.isActive) "Active" else "Inactive")
                                Switch(checked = user.isActive, onCheckedChange = { viewModel.toggleUser(user) })
                            }
                        }
                    )
                }
            }
        }
        FloatingActionButton(onClick = { creating = true }, Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Default.Add, "Add user")
        }
    }
    if (creating) AddUserDialog({ creating = false }) { username, name, role, password ->
        viewModel.addUser(username, name, role, password); creating = false
    }
}

@Composable
private fun AddUserDialog(onDismiss: () -> Unit, onSave: (String, String, UserRole, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.EMPLOYEE) }
    val valid = username.length >= 3 && displayName.isNotBlank() && password.length >= 8
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add user") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(username, { username = it.filterNot(Char::isWhitespace) }, label = { Text("Username") }, singleLine = true)
                OutlinedTextField(displayName, { displayName = it }, label = { Text("Display name") }, singleLine = true)
                OutlinedTextField(password, { password = it }, label = { Text("Temporary password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Text("Role", fontWeight = FontWeight.Bold)
                UserRole.entries.forEach { item ->
                    Row(Modifier.fillMaxWidth().clickable { role = item }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(role == item, onClick = { role = item })
                        Text(item.displayName())
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(username, displayName, role, password) }, enabled = valid) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun UserRole.displayName(): String = when (this) {
    UserRole.SUPER_USER -> "Super User"
    UserRole.ADMIN -> "Admin"
    UserRole.EMPLOYEE -> "Employee"
}

private fun formatDate(epoch: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(epoch))
