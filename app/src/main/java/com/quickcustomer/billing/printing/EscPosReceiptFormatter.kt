package com.quickcustomer.billing.printing

import com.quickcustomer.billing.data.PaymentMethod
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EscPosReceiptFormatter {
    private val initialize = byteArrayOf(0x1B, 0x40)
    private val alignLeft = byteArrayOf(0x1B, 0x61, 0x00)
    private val alignCenter = byteArrayOf(0x1B, 0x61, 0x01)
    private val boldOn = byteArrayOf(0x1B, 0x45, 0x01)
    private val boldOff = byteArrayOf(0x1B, 0x45, 0x00)
    private val fullCut = byteArrayOf(0x1D, 0x56, 0x00)

    fun format(receipt: PrintableReceipt, paperWidthMm: Int): ByteArray {
        val width = if (paperWidthMm >= 80) 48 else 32
        val output = ByteArrayOutputStream()

        output.write(initialize)
        output.write(alignCenter)
        output.write(boldOn)
        output.text(receipt.shopName.uppercase(Locale.getDefault()))
        output.write(boldOff)
        receipt.address.takeIf(String::isNotBlank)?.let { output.wrapped(it, width) }
        receipt.phone.takeIf(String::isNotBlank)?.let { output.wrapped("Phone: $it", width) }
        output.text("-".repeat(width))

        if (receipt.isCancelled) {
            output.write(boldOn)
            output.text("CANCELLED COPY")
            output.write(boldOff)
        }

        output.write(alignLeft)
        output.pair("Bill", receipt.invoiceNumber, width)
        output.pair("Date", formatDate(receipt.createdAt), width)
        output.pair("Cashier", receipt.cashierName, width)
        output.text("-".repeat(width))

        receipt.items.forEach { item ->
            output.wrapped("${item.quantity} x ${item.name}", width)
            output.pair("  @ ${money(item.unitPricePaise)}", money(item.lineTotalPaise), width)
        }

        output.text("-".repeat(width))
        output.pair("Subtotal", money(receipt.subtotalPaise), width)
        if (receipt.discountPaise > 0) output.pair("Discount", "-${money(receipt.discountPaise)}", width)
        if (receipt.taxPaise > 0) output.pair("Tax", money(receipt.taxPaise), width)
        output.write(boldOn)
        output.pair("TOTAL", money(receipt.totalPaise), width)
        output.write(boldOff)
        output.pair("Payment", receipt.paymentMethod.name, width)
        if (receipt.paymentMethod == PaymentMethod.CASH) {
            receipt.cashReceivedPaise?.let { output.pair("Cash received", money(it), width) }
            receipt.changeReturnedPaise?.let { output.pair("Change", money(it), width) }
        }

        receipt.footer.takeIf(String::isNotBlank)?.let {
            output.text("-".repeat(width))
            output.write(alignCenter)
            output.wrapped(it, width)
        }
        output.text("")
        output.text("")
        output.text("")
        output.write(fullCut)
        return output.toByteArray()
    }

    fun testPage(paperWidthMm: Int): ByteArray {
        val width = if (paperWidthMm >= 80) 48 else 32
        val output = ByteArrayOutputStream()
        output.write(initialize)
        output.write(alignCenter)
        output.write(boldOn)
        output.text("QUICK CUSTOMER")
        output.write(boldOff)
        output.text("Printer test successful")
        output.text("Paper width: ${if (paperWidthMm >= 80) 80 else 58} mm")
        output.text("-".repeat(width))
        output.text("Generic ESC/POS Bluetooth")
        output.text("")
        output.text("")
        output.text("")
        output.write(fullCut)
        return output.toByteArray()
    }

    private fun ByteArrayOutputStream.text(value: String) {
        write(ascii(value).toByteArray(Charsets.US_ASCII))
        write('\n'.code)
    }

    private fun ByteArrayOutputStream.wrapped(value: String, width: Int) {
        wrap(ascii(value), width).forEach { line -> text(line) }
    }

    private fun ByteArrayOutputStream.pair(left: String, right: String, width: Int) {
        val safeLeft = ascii(left)
        val safeRight = ascii(right)
        if (safeLeft.length + safeRight.length + 1 <= width) {
            text(safeLeft + " ".repeat(width - safeLeft.length - safeRight.length) + safeRight)
        } else {
            wrapped(safeLeft, width)
            text(safeRight.takeLast(width).padStart(width))
        }
    }

    private fun wrap(value: String, width: Int): List<String> {
        if (value.isBlank()) return listOf("")
        val result = mutableListOf<String>()
        var remaining = value.trim()
        while (remaining.length > width) {
            val candidate = remaining.take(width)
            val breakAt = candidate.lastIndexOf(' ').takeIf { it > 0 } ?: width
            result += remaining.take(breakAt).trimEnd()
            remaining = remaining.drop(breakAt).trimStart()
        }
        result += remaining
        return result
    }

    private fun ascii(value: String): String = value
        .replace("₹", "Rs ")
        .replace("×", "x")
        .replace("−", "-")
        .replace("•", "-")
        .map { if (it.code in 32..126) it else '?' }
        .joinToString("")

    private fun money(paise: Long): String = "Rs ${paise / 100}.${(paise % 100).toString().padStart(2, '0')}"

    private fun formatDate(epoch: Long): String =
        SimpleDateFormat("dd MMM yy HH:mm", Locale.getDefault()).format(Date(epoch))
}
