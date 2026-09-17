package com.quickcustomer.billing.sync

import kotlinx.serialization.Serializable

enum class DeviceMode { PRIMARY, MONITOR }

enum class DriveSetupStage {
    REQUIRED,
    AUTHORIZING,
    CHECKING_DRIVE,
    DRIVE_EMPTY,
    RESTORING,
    READY,
    LOCAL_ONLY,
    ERROR
}

data class DriveUiState(
    val stage: DriveSetupStage = DriveSetupStage.REQUIRED,
    val email: String = "",
    val deviceMode: DeviceMode? = null,
    val lastSyncAt: Long = 0,
    val message: String = ""
) {
    val isConnected: Boolean get() = stage == DriveSetupStage.READY
}

@Serializable
data class DriveStoreManifest(
    val formatVersion: Int = CURRENT_FORMAT,
    val storeId: String,
    val primaryDeviceId: String,
    val shopName: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val CURRENT_FORMAT = 1
    }
}

data class DriveStoreFiles(
    val manifestFileId: String,
    val manifest: DriveStoreManifest,
    val snapshotFileId: String
)

data class DriveConnection(
    val email: String,
    val storeId: String,
    val mode: DeviceMode,
    val primaryDeviceId: String
)
