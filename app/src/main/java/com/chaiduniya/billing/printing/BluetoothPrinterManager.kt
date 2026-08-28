package com.chaiduniya.billing.printing

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.chaiduniya.billing.data.ShopSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class PairedBluetoothPrinter(
    val name: String,
    val address: String
)

class BluetoothPrinterManager(private val context: Context) {
    private val adapter: BluetoothAdapter?
        get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    fun hasConnectPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun pairedPrinters(): List<PairedBluetoothPrinter> {
        require(hasConnectPermission()) { "Allow Nearby devices permission to use the printer." }
        val bluetooth = adapter ?: error("Bluetooth is not available on this device.")
        require(bluetooth.isEnabled) { "Turn on Bluetooth, then try again." }
        return bluetooth.bondedDevices
            .map { device ->
                PairedBluetoothPrinter(
                    name = device.name?.takeIf(String::isNotBlank) ?: "Bluetooth device",
                    address = device.address
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun print(receipt: PrintableReceipt, settings: ShopSettingsEntity) {
        require(settings.printerEnabled) { "Enable the Bluetooth printer in Settings." }
        require(settings.printerAddress.isNotBlank()) { "Select a paired printer in Settings." }
        send(settings.printerAddress, EscPosReceiptFormatter.format(receipt, settings.printerPaperWidthMm))
    }

    suspend fun printTest(settings: ShopSettingsEntity) {
        require(settings.printerAddress.isNotBlank()) { "Select a paired printer first." }
        send(settings.printerAddress, EscPosReceiptFormatter.testPage(settings.printerPaperWidthMm))
    }

    @SuppressLint("MissingPermission")
    private suspend fun send(address: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        require(hasConnectPermission()) { "Allow Nearby devices permission to use the printer." }
        val bluetooth = adapter ?: error("Bluetooth is not available on this device.")
        require(bluetooth.isEnabled) { "Turn on Bluetooth, then try again." }
        val device = runCatching { bluetooth.getRemoteDevice(address) }
            .getOrElse { error("The selected printer is no longer available. Select it again in Settings.") }

        val attempts = listOf(
            { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
            { device.createRfcommSocketToServiceRecord(SPP_UUID) }
        )
        var lastFailure: Throwable? = null
        for (socketFactory in attempts) {
            val socket = try {
                socketFactory()
            } catch (failure: Throwable) {
                lastFailure = failure
                continue
            }
            try {
                socket.connect()
                socket.outputStream.use { output ->
                    output.write(bytes)
                    output.flush()
                    Thread.sleep(200)
                }
                socket.close()
                return@withContext
            } catch (failure: Throwable) {
                lastFailure = failure
                runCatching { socket.close() }
            }
        }
        throw IllegalStateException(
            "Could not connect to ${device.name ?: "the printer"}. Check that it is on, paired, and not connected to another device.",
            lastFailure
        )
    }

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
