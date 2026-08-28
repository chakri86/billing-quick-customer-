package com.chaiduniya.billing.printing

import com.chaiduniya.billing.data.PaymentMethod
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EscPosReceiptFormatterTest {
    private val receipt = PrintableReceipt(
        shopName = "Quick Customer",
        address = "Main Road",
        phone = "9999999999",
        invoiceNumber = "QC-1001",
        createdAt = 0L,
        cashierName = "Cashier",
        paymentMethod = PaymentMethod.CASH,
        items = listOf(
            PrintableReceiptItem(
                name = "A very long masala chai product name",
                quantity = 2,
                unitPricePaise = 2_000,
                lineTotalPaise = 4_000
            )
        ),
        subtotalPaise = 4_000,
        discountPaise = 0,
        taxPaise = 0,
        totalPaise = 4_000,
        cashReceivedPaise = 10_000,
        changeReturnedPaise = 6_000,
        footer = "Thank you"
    )

    @Test
    fun receiptContainsImportantBillingInformation() {
        val text = EscPosReceiptFormatter.format(receipt, 58).toString(Charsets.US_ASCII)

        assertTrue(text.contains("QUICK CUSTOMER"))
        assertTrue(text.contains("QC-1001"))
        assertTrue(text.contains("2 x A very long masala chai"))
        assertTrue(text.contains("TOTAL"))
        assertTrue(text.contains("Rs 40.00"))
        assertTrue(text.contains("Cash received"))
        assertTrue(text.contains("Rs 100.00"))
        assertTrue(text.contains("Change"))
        assertTrue(text.contains("Rs 60.00"))
    }

    @Test
    fun formatterUsesPrinterSafeAsciiInsteadOfRupeeSymbol() {
        val text = EscPosReceiptFormatter.format(receipt, 80).toString(Charsets.US_ASCII)

        assertFalse(text.contains("₹"))
        assertTrue(text.contains("Rs "))
    }

    @Test
    fun cancelledHistoricalBillIsClearlyMarked() {
        val text = EscPosReceiptFormatter.format(receipt.copy(isCancelled = true), 58)
            .toString(Charsets.US_ASCII)

        assertTrue(text.contains("CANCELLED COPY"))
    }
}
