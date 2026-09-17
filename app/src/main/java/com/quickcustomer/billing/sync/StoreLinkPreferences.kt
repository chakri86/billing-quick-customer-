package com.quickcustomer.billing.sync

import android.content.Context
import java.util.UUID

class StoreLinkPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val deviceId: String
        get() = preferences.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString(KEY_DEVICE_ID, it).apply()
        }

    val isLinked: Boolean get() = preferences.getBoolean(KEY_LINKED, false)
    val email: String get() = preferences.getString(KEY_EMAIL, "").orEmpty()
    val storeId: String get() = preferences.getString(KEY_STORE_ID, "").orEmpty()
    val primaryDeviceId: String get() = preferences.getString(KEY_PRIMARY_DEVICE_ID, "").orEmpty()
    val mode: DeviceMode?
        get() = preferences.getString(KEY_DEVICE_MODE, null)?.let {
            runCatching { DeviceMode.valueOf(it) }.getOrNull()
        }
    val lastSyncAt: Long get() = preferences.getLong(KEY_LAST_SYNC_AT, 0)

    fun saveConnection(connection: DriveConnection, syncedAt: Long) {
        preferences.edit()
            .putBoolean(KEY_LINKED, true)
            .putString(KEY_EMAIL, connection.email)
            .putString(KEY_STORE_ID, connection.storeId)
            .putString(KEY_DEVICE_MODE, connection.mode.name)
            .putString(KEY_PRIMARY_DEVICE_ID, connection.primaryDeviceId)
            .putLong(KEY_LAST_SYNC_AT, syncedAt)
            .apply()
    }

    fun updateLastSync(at: Long) {
        preferences.edit().putLong(KEY_LAST_SYNC_AT, at).apply()
    }

    fun clearConnection() {
        val stableDeviceId = deviceId
        preferences.edit().clear().putString(KEY_DEVICE_ID, stableDeviceId).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "quick-customer-drive-link"
        private const val KEY_DEVICE_ID = "device-id"
        private const val KEY_LINKED = "linked"
        private const val KEY_EMAIL = "email"
        private const val KEY_STORE_ID = "store-id"
        private const val KEY_DEVICE_MODE = "device-mode"
        private const val KEY_PRIMARY_DEVICE_ID = "primary-device-id"
        private const val KEY_LAST_SYNC_AT = "last-sync-at"
    }
}
