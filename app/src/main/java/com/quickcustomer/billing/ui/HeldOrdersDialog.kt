package com.quickcustomer.billing.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quickcustomer.billing.data.HeldOrder
import com.quickcustomer.billing.data.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun HeldOrdersDialog(viewModel: BillingViewModel, onDismiss: () -> Unit) {
    val all by viewModel.heldOrders.collectAsState()
    val user = viewModel.currentUser
    var cancelled by remember { mutableStateOf(false) }
    var cancelOrder by remember { mutableStateOf<HeldOrder?>(null) }
    var reason by remember { mutableStateOf("") }
    val orders = all.filter {
        (user?.role != UserRole.EMPLOYEE || it.cashierId == user.id) &&
            it.status == if (cancelled) "CANCELLED" else "HELD"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saved orders") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !cancelled, onClick = { cancelled = false }, label = { Text("Held") })
                    FilterChip(selected = cancelled, onClick = { cancelled = true }, label = { Text("Cancelled") })
                }
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (orders.isEmpty()) Text(if (cancelled) "No cancelled held orders." else "No held orders.")
                    orders.forEach { order ->
                        val lines = remember(order.linesJson) { order.lines() }
                        Text(order.label.ifBlank { "Order ${order.id.take(8)}" }, style = MaterialTheme.typography.titleMedium)
                        Text("${SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(order.createdAt))} · ${order.cashierName}")
                        lines.forEach { Text("${it.quantity} × ${it.product.name}") }
                        Text("₹%.2f".format(Locale.US, lines.sumOf { it.lineTotalPaise } / 100.0))
                        if (cancelled) {
                            Text("${order.cancellationReason} · ${order.cancelledByName}")
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled = !viewModel.isSaving, onClick = {
                                    viewModel.resumeHeldOrder(order)
                                    onDismiss()
                                }) { Text("Resume") }
                                TextButton(enabled = !viewModel.isSaving, onClick = { cancelOrder = order; reason = "" }) {
                                    Text("Cancel order")
                                }
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
    cancelOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { cancelOrder = null },
            title = { Text("Cancel held order?") },
            text = { OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason (optional)") }) },
            confirmButton = {
                TextButton(enabled = !viewModel.isSaving, onClick = {
                    viewModel.cancelHeldOrder(order, reason)
                    cancelOrder = null
                }) { Text("Cancel order") }
            },
            dismissButton = { TextButton(onClick = { cancelOrder = null }) { Text("Keep order") } }
        )
    }
}
