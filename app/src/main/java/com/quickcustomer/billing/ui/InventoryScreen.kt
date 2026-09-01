package com.quickcustomer.billing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quickcustomer.billing.data.InventoryItemEntity
import com.quickcustomer.billing.data.InventoryStock
import com.quickcustomer.billing.data.InventoryUnit
import com.quickcustomer.billing.data.PaymentMethod
import com.quickcustomer.billing.data.ProductEntity
import com.quickcustomer.billing.data.RecipeIngredientDetail
import com.quickcustomer.billing.data.StockTransactionType
import com.quickcustomer.billing.data.UserEntity
import com.quickcustomer.billing.data.UserRole
import com.quickcustomer.billing.domain.Money
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class InventoryTab(val label: String) { STOCK("Stock"), RECIPES("Product recipes"), HISTORY("History") }

@Composable
fun InventoryScreen(viewModel: BillingViewModel, user: UserEntity) {
    val stock by viewModel.inventoryStock.collectAsState()
    val transactions by viewModel.stockTransactions.collectAsState()
    val recipes by viewModel.recipeDetails.collectAsState()
    val products by viewModel.products.collectAsState()
    var tab by remember { mutableStateOf(InventoryTab.STOCK) }
    var adding by remember { mutableStateOf(false) }
    var purchasing by remember { mutableStateOf<InventoryStock?>(null) }
    var adjusting by remember { mutableStateOf<InventoryStock?>(null) }
    var recipeProduct by remember { mutableStateOf<ProductEntity?>(null) }
    val manager = user.role != UserRole.EMPLOYEE
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Inventory", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                items(InventoryTab.entries) { item -> FilterChip(selected = tab == item, onClick = { tab = item }, label = { Text(item.label) }) }
            }
            when (tab) {
                InventoryTab.STOCK -> {
                    if (stock.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (manager) "Add your first inventory item" else "No inventory items configured") }
                    else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(stock, key = { it.item.id }) { row ->
                            val low = row.currentStockMilli <= row.item.minimumStockMilli
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth()) {
                                        Text(row.item.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                        Text(formatQuantity(row.currentStockMilli, row.item.unit), fontWeight = FontWeight.Bold, color = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                    }
                                    Text("Low-stock level: ${formatQuantity(row.item.minimumStockMilli, row.item.unit)}")
                                    Text("Average cost: ${Money.format(row.item.averageCostPaisePerUnit)} per ${row.item.unit.name.lowercase()}")
                                    if (low) Text("Low stock — billing remains available", color = MaterialTheme.colorScheme.error)
                                    if (manager) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { purchasing = row }) { Text("Purchase") }
                                        OutlinedButton(onClick = { adjusting = row }) { Text("Adjust") }
                                    }
                                }
                            }
                        }
                    }
                }
                InventoryTab.RECIPES -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products.filter { !it.isDeleted }, key = { it.id }) { product ->
                        val linked = recipes.filter { it.productId == product.id }
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold); Text(if (linked.isEmpty()) "Cost not configured" else linked.joinToString { "${it.inventoryName}: ${formatQuantity(it.quantityMilliPerSaleUnit, it.unit)}" }) }
                                if (manager) TextButton(onClick = { recipeProduct = product }) { Text("Edit") }
                            }
                        } }
                    }
                }
                InventoryTab.HISTORY -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(transactions, key = { it.id }) { tx ->
                        val item = stock.firstOrNull { it.item.id == tx.inventoryItemId }?.item
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth()) { Text(item?.name ?: "Inventory item", Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text(tx.type.name.replace('_', ' ')) }
                            Text("${if (tx.quantityDeltaMilli > 0) "+" else ""}${formatQuantity(tx.quantityDeltaMilli, item?.unit ?: InventoryUnit.PIECE)} • ${formatInventoryDate(tx.createdAt)}")
                            if (tx.description.isNotBlank()) Text(tx.description)
                        } }
                    }
                }
            }
        }
        if (manager && tab == InventoryTab.STOCK) FloatingActionButton(onClick = { adding = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) { androidx.compose.material3.Icon(Icons.Default.Add, "Add inventory item") }
    }
    if (adding) AddInventoryDialog(onDismiss = { adding = false }) { name, unit, minimum, opening -> viewModel.addInventoryItem(name, unit, minimum, opening); adding = false }
    purchasing?.let { row -> PurchaseDialog(row, onDismiss = { purchasing = null }) { qty, cost, payment, supplier, description -> viewModel.purchaseStock(row.item, qty, cost, payment, supplier, description); purchasing = null } }
    adjusting?.let { row -> AdjustDialog(row, onDismiss = { adjusting = null }) { type, qty, reason -> viewModel.adjustStock(row.item, type, qty, reason); adjusting = null } }
    recipeProduct?.let { product -> RecipeDialog(product, stock.map { it.item }, recipes.filter { it.productId == product.id }, onDismiss = { recipeProduct = null }, onSave = viewModel::saveRecipeIngredient, onDelete = viewModel::deleteRecipeIngredient) }
}

@Composable
private fun AddInventoryDialog(onDismiss: () -> Unit, onSave: (String, InventoryUnit, Long, Long) -> Unit) {
    var name by remember { mutableStateOf("") }; var unit by remember { mutableStateOf(InventoryUnit.PIECE) }; var minimum by remember { mutableStateOf("") }; var opening by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add inventory item") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Item name") }, modifier = Modifier.fillMaxWidth())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(InventoryUnit.entries) { item -> FilterChip(selected = unit == item, onClick = { unit = item }, label = { Text(item.name.lowercase().replaceFirstChar { it.uppercase() }) }) } }
        NumberField(minimum, { minimum = it }, "Low-stock level in ${unit.name.lowercase()}")
        NumberField(opening, { opening = it }, "Opening stock in ${unit.name.lowercase()}")
    } }, confirmButton = { Button(onClick = { onSave(name, unit, toMilli(minimum), toMilli(opening)) }, enabled = name.isNotBlank()) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun PurchaseDialog(row: InventoryStock, onDismiss: () -> Unit, onSave: (Long, Long, PaymentMethod, String, String) -> Unit) {
    var quantity by remember { mutableStateOf("") }; var cost by remember { mutableStateOf("") }; var payment by remember { mutableStateOf(PaymentMethod.CASH) }; var supplier by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }
    val costPaise = ((cost.toDoubleOrNull() ?: 0.0) * 100).toLong()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Purchase ${row.item.name}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(quantity, { quantity = it }, "Quantity in ${row.item.unit.name.lowercase()}")
        NumberField(cost, { cost = it }, "Total cost in ₹")
        Row { PaymentMethod.entries.forEach { item -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(payment == item, { payment = item }); Text(item.name) } } }
        OutlinedTextField(supplier, { supplier = it }, label = { Text("Supplier (optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())
        Text("Saving this purchase also creates an approved expense.", style = MaterialTheme.typography.bodySmall)
    } }, confirmButton = { Button(onClick = { onSave(toMilli(quantity), costPaise, payment, supplier, description) }, enabled = toMilli(quantity) > 0 && costPaise > 0) { Text("Save purchase") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun AdjustDialog(row: InventoryStock, onDismiss: () -> Unit, onSave: (StockTransactionType, Long, String) -> Unit) {
    var type by remember { mutableStateOf(StockTransactionType.ADJUSTMENT) }; var quantity by remember { mutableStateOf("") }; var reason by remember { mutableStateOf("") }
    val types = listOf(StockTransactionType.ADJUSTMENT, StockTransactionType.WASTAGE, StockTransactionType.SUPPLIER_RETURN)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Adjust ${row.item.name}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(types) { item -> FilterChip(selected = type == item, onClick = { type = item }, label = { Text(item.name.replace('_', ' ')) }) } }
        NumberField(quantity, { quantity = it }, if (type == StockTransactionType.ADJUSTMENT) "Quantity (+ add, − remove)" else "Quantity to remove")
        OutlinedTextField(reason, { reason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth())
    } }, confirmButton = { Button(onClick = { onSave(type, toMilli(quantity), reason) }, enabled = toMilli(quantity) != 0L && reason.trim().length >= 3) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun RecipeDialog(product: ProductEntity, inventory: List<InventoryItemEntity>, linked: List<RecipeIngredientDetail>, onDismiss: () -> Unit, onSave: (String, String, Long) -> Unit, onDelete: (String, String) -> Unit) {
    var selected by remember(inventory) { mutableStateOf(inventory.firstOrNull()) }; var quantity by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Recipe: ${product.name}") }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (inventory.isEmpty()) item { Text("Add inventory items before configuring recipes.") }
        items(linked, key = { it.inventoryItemId }) { item -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("${item.inventoryName}: ${formatQuantity(item.quantityMilliPerSaleUnit, item.unit)}", Modifier.weight(1f)); TextButton(onClick = { onDelete(product.id, item.inventoryItemId) }) { Text("Remove") } } }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(inventory) { item -> FilterChip(selected = selected?.id == item.id, onClick = { selected = item }, label = { Text(item.name) }) } } }
        item { NumberField(quantity, { quantity = it }, "Quantity per sale in ${selected?.unit?.name?.lowercase() ?: "units"}") }
        item { Button(onClick = { selected?.let { onSave(product.id, it.id, toMilli(quantity)); quantity = "" } }, enabled = selected != null && toMilli(quantity) > 0) { Text("Add / update ingredient") } }
    } }, confirmButton = { Button(onClick = onDismiss) { Text("Done") } })
}

@Composable private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String) = OutlinedTextField(value, onValueChange, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
private fun toMilli(value: String): Long = ((value.toDoubleOrNull() ?: 0.0) * 1_000).toLong()
private fun formatQuantity(milli: Long, unit: InventoryUnit): String { val value = milli / 1_000.0; val number = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.'); return "$number ${unit.name.lowercase()}" }
private fun formatInventoryDate(epoch: Long): String = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(epoch))
