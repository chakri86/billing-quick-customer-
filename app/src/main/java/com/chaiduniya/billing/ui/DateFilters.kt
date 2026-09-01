@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chaiduniya.billing.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class DateWindow(val startInclusive: Long, val endExclusive: Long) {
    fun contains(timestamp: Long): Boolean = timestamp >= startInclusive && timestamp < endExclusive
}

fun todayWindow(): DateWindow = dateWindow(LocalDate.now(), LocalDate.now())

fun recentDaysWindow(days: Long): DateWindow {
    val today = LocalDate.now()
    return dateWindow(today.minusDays((days - 1).coerceAtLeast(0)), today)
}

fun dateWindow(start: LocalDate, endInclusive: LocalDate): DateWindow {
    val safeEnd = if (endInclusive < start) start else endInclusive
    val zone = ZoneId.systemDefault()
    return DateWindow(
        start.atStartOfDay(zone).toInstant().toEpochMilli(),
        safeEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    )
}

fun expenseTimestampFor(date: LocalDate): Long = date
    .atTime(LocalTime.NOON)
    .atZone(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()

fun formatDateOnly(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()
    .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

fun formatDateWindow(window: DateWindow): String =
    "${formatDateOnly(window.startInclusive)} – ${formatDateOnly(window.endExclusive - 1)}"

private fun localTimestampToPickerMillis(timestamp: Long): Long {
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun pickerMillisToLocalDate(timestamp: Long): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
fun CustomDateRangeDialog(
    initialWindow: DateWindow,
    onDismiss: () -> Unit,
    onConfirm: (DateWindow) -> Unit
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = localTimestampToPickerMillis(initialWindow.startInclusive),
        initialSelectedEndDateMillis = localTimestampToPickerMillis(initialWindow.endExclusive - 1)
    )
    val start = state.selectedStartDateMillis
    val end = state.selectedEndDateMillis
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val startDate = pickerMillisToLocalDate(requireNotNull(start))
                    val endDate = pickerMillisToLocalDate(requireNotNull(end))
                    onConfirm(dateWindow(startDate, endDate))
                },
                enabled = start != null && end != null && end >= start
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(bottom = 8.dp)) {
            DateRangePicker(state = state, showModeToggle = false)
        }
    }
}

@Composable
fun ExpenseDatePickerDialog(
    initialTimestamp: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val todayPickerMillis = localTimestampToPickerMillis(System.currentTimeMillis())
    val state = rememberDatePickerState(
        initialSelectedDateMillis = localTimestampToPickerMillis(initialTimestamp)
    )
    val selected = state.selectedDateMillis
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val date = pickerMillisToLocalDate(requireNotNull(selected))
                    onConfirm(expenseTimestampFor(date))
                },
                enabled = selected != null && selected <= todayPickerMillis
            ) { Text("Select") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}
