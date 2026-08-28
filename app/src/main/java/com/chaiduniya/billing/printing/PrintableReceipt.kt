package com.chaiduniya.billing.printing

import com.chaiduniya.billing.data.BillDetails
import com.chaiduniya.billing.data.PaymentMethod
import com.chaiduniya.billing.data.Receipt

data class PrintableReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPricePaise: Long,
    val lineTotalPaise: Long
)

data class PrintableReceipt(
    val shopName: String,
    val address: String,
    val phone: String,
    val invoiceNumber: String,
    val createdAt: Long,
    val cashierName: String,
    val paymentMethod: PaymentMethod,
    val items: List<PrintableReceiptItem>,
    val subtotalPaise: Long,
    val discountPaise: Long,
    val taxPaise: Long,
    val totalPaise: Long,
    val cashReceivedPaise: Long?,
    val changeReturnedPaise: Long?,
    val footer: String,
    val isCancelled: Boolean = false
) {
    companion object {
        fun from(receipt: Receipt): PrintableReceipt = PrintableReceipt(
            shopName = receipt.settings.shopName,
            address = receipt.settings.address,
            phone = receipt.settings.phone,
            invoiceNumber = receipt.sale.invoiceNumber,
            createdAt = receipt.sale.createdAt,
            cashierName = receipt.sale.cashierName,
            paymentMethod = receipt.sale.paymentMethod,
            items = receipt.lines.map { line ->
                PrintableReceiptItem(
                    name = line.product.name,
                    quantity = line.quantity,
                    unitPricePaise = line.product.pricePaise,
                    lineTotalPaise = line.lineTotalPaise
                )
            },
            subtotalPaise = receipt.sale.subtotalPaise,
            discountPaise = receipt.sale.discountPaise,
            taxPaise = receipt.sale.taxPaise,
            totalPaise = receipt.sale.totalPaise,
            cashReceivedPaise = receipt.sale.cashReceivedPaise,
            changeReturnedPaise = receipt.sale.changeReturnedPaise,
            footer = receipt.settings.receiptFooter,
            isCancelled = receipt.sale.isCancelled
        )

        fun from(details: BillDetails): PrintableReceipt = PrintableReceipt(
            shopName = details.settings.shopName,
            address = details.settings.address,
            phone = details.settings.phone,
            invoiceNumber = details.sale.invoiceNumber,
            createdAt = details.sale.createdAt,
            cashierName = details.sale.cashierName,
            paymentMethod = details.sale.paymentMethod,
            items = details.items.map { item ->
                PrintableReceiptItem(
                    name = item.productNameSnapshot,
                    quantity = item.quantity,
                    unitPricePaise = item.unitPricePaise,
                    lineTotalPaise = item.lineTotalPaise
                )
            },
            subtotalPaise = details.sale.subtotalPaise,
            discountPaise = details.sale.discountPaise,
            taxPaise = details.sale.taxPaise,
            totalPaise = details.sale.totalPaise,
            cashReceivedPaise = details.sale.cashReceivedPaise,
            changeReturnedPaise = details.sale.changeReturnedPaise,
            footer = details.settings.receiptFooter,
            isCancelled = details.sale.isCancelled
        )
    }
}
