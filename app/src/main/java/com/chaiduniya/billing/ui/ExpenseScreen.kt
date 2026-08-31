package com.chaiduniya.billing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.chaiduniya.billing.data.ExpenseCategories
import com.chaiduniya.billing.data.ExpenseEntity
import com.chaiduniya.billing.data.ExpenseStatus
import com.chaiduniya.billing.data.PaymentMethod
import com.chaiduniya.billing.data.UserEntity
import com.chaiduniya.billing.data.UserRole
import com.chaiduniya.billing.domain.Money
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ExpensePeriod(val label: String, val days: Int?) {
    TODAY("Today", 0), WEEK("7 days", 7), MONTH("30 days", 30), ALL("All time", null)
}

@Composable
fun ExpenseScreen(viewModel: BillingViewModel, user: UserEntity) {
    val allExpenses by viewModel.expenses.collectAsState()
    val allSales by viewModel.sales.collectAsState()
    var period by remember { mutableStateOf(ExpensePeriod.TODAY) }
    var adding by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf<Pair<ExpenseEntity, String>?>(null) }
    val now = System.currentTimeMillis()
    val cutoff = when (period.days) {
        null -> null
        0 -> java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        else -> now - period.days!! * 86_400_000L
    }
    val visible = allExpenses.filter {
        (user.role != UserRole.EMPLOYEE || it.enteredById == user.id) && (cutoff == null || it.occurredAt >= cutoff)
    }
    val approved = visible.filter { it.status == ExpenseStatus.APPROVED }
    val sales = allSales.filter {
        !it.isCancelled && (user.role != UserRole.EMPLOYEE || it.cashierId == user.id) && (cutoff == null || it.createdAt >= cutoff)
    }
    val salesTotal = sales.sumOf { it.totalPaise }
    val expenseTotal = approved.sumOf { it.amountPaise }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(if (user.role == UserRole.EMPLOYEE) "My expenses" else "Shop expenses", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                items(ExpensePeriod.entries) { item ->
                    FilterChip(selected = period == item, onClick = { period = item }, label = { Text(item.label) })
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { ExpenseSummary("Sales", Money.format(salesTotal)) }
                item { ExpenseSummary("Approved expenses", Money.format(expenseTotal)) }
                item { ExpenseSummary("Sales − expenses", Money.format(salesTotal - expenseTotal)) }
                item { ExpenseSummary("Pending", visible.count { it.status == ExpenseStatus.PENDING }.toString()) }
            }
            Spacer(Modifier.height(14.dp))
            if (visible.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No expenses in this period") }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visible, key = { it.id }) { expense ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(Modifier.fillMaxWidth()) {
                                    Text(expense.category, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    Text(Money.format(expense.amountPaise), fontWeight = FontWeight.Bold)
                                }
                                Text("${expense.enteredByName} • ${expense.paymentMethod.name} • ${formatExpenseDate(expense.occurredAt)}")
                                if (expense.supplierName.isNotBlank()) Text("Supplier/paid to: ${expense.supplierName}")
                                if (expense.description.isNotBlank()) Text(expense.description)
                                Text("Status: ${expense.status.name}", color = when (expense.status) {
                                    ExpenseStatus.APPROVED -> MaterialTheme.colorScheme.primary
                                    ExpenseStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                })
                                if (user.role != UserRole.EMPLOYEE && expense.status == ExpenseStatus.PENDING) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { viewModel.approveExpense(expense) }) { Text("Approve") }
                                        OutlinedButton(onClick = { action = expense to "REJECT" }) { Text("Reject") }
                                    }
                                }
                                if (user.role != UserRole.EMPLOYEE && expense.status == ExpenseStatus.APPROVED && expense.linkedStockTransactionId == null) {
                                    TextButton(onClick = { action = expense to "CANCEL" }) { Text("Cancel expense") }
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { adding = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            androidx.compose.material3.Icon(Icons.Default.Add, "Add expense")
        }
    }
    if (adding) AddExpenseDialog(onDismiss = { adding = false }) { category, amount, method, supplier, description ->
        viewModel.addExpense(category, amount, method, supplier, description)
        adding = false
    }
    action?.let { (expense, type) ->
        ExpenseReasonDialog(type, onDismiss = { action = null }) { reason ->
            if (type == "REJECT") viewModel.rejectExpense(expense, reason) else viewModel.cancelExpense(expense, reason)
            action = null
        }
    }
}

@Composable
private fun ExpenseSummary(label: String, value: String) {
    Card(Modifier.width(180.dp)) { Column(Modifier.padding(12.dp)) { Text(label); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun AddExpenseDialog(onDismiss: () -> Unit, onSave: (String, Long, PaymentMethod, String, String) -> Unit) {
    var category by remember { mutableStateOf(ExpenseCategories.defaults.first()) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var supplier by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val paise = ((amount.toDoubleOrNull() ?: 0.0) * 100).toLong()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expense") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Category", fontWeight = FontWeight.SemiBold) }
                item { LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(ExpenseCategories.defaults) { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) }) } } }
                item { OutlinedTextField(amount, { amount = it }, label = { Text("Amount in ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
                item {
                    Row { PaymentMethod.entries.forEach { item -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(method == item, { method = item }); Text(item.name) } } }
                }
                item { OutlinedTextField(supplier, { supplier = it }, label = { Text("Supplier / paid to (optional)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth()) }
                item { Text("Employee entries are sent to an Admin or Super User for approval.", style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { Button(onClick = { onSave(category, paise, method, supplier, description) }, enabled = paise > 0) { Text("Save expense") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ExpenseReasonDialog(type: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (type == "REJECT") "Reject expense" else "Cancel expense") },
        text = { OutlinedTextField(reason, { reason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = { onConfirm(reason.trim()) }, enabled = reason.trim().length >= 3) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back") } })
}

private fun formatExpenseDate(epoch: Long): String = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(epoch))
