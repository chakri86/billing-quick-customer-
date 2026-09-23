package com.quickcustomer.billing.sync

import android.content.Context
import com.quickcustomer.billing.data.BillingRepository
import java.util.UUID

sealed interface DriveConnectOutcome {
    data class ExistingStore(
        val connection: DriveConnection,
        val syncedAt: Long,
        val shopName: String
    ) : DriveConnectOutcome

    data class EmptyDrive(
        val connection: DriveConnection
    ) : DriveConnectOutcome
}

data class DriveSyncOutcome(
    val syncedAt: Long,
    val uploaded: Boolean,
    val shopName: String
)

class DriveSyncManager(
    context: Context,
    private val repository: BillingRepository,
    private val client: GoogleDriveStoreClient = GoogleDriveStoreClient()
) {
    val preferences = StoreLinkPreferences(context)

    suspend fun connect(accessToken: String): DriveConnectOutcome {
        val email = client.accountEmail(accessToken)
        val previouslyLinkedEmail = preferences.email
        require(previouslyLinkedEmail.isBlank() || previouslyLinkedEmail.equals(email, ignoreCase = true)) {
            "This device is linked to $previouslyLinkedEmail. Select that store Gmail or disconnect it first."
        }

        val remote = client.findStore(accessToken)
        if (remote == null) {
            val connection = DriveConnection(
                email = email,
                storeId = preferences.storeId.ifBlank { UUID.randomUUID().toString() },
                mode = DeviceMode.PRIMARY,
                primaryDeviceId = preferences.deviceId
            )
            preferences.saveConnection(connection, syncedAt = 0)
            return DriveConnectOutcome.EmptyDrive(connection)
        }

        val mode = if (remote.manifest.primaryDeviceId == preferences.deviceId) {
            DeviceMode.PRIMARY
        } else {
            DeviceMode.MONITOR
        }
        if (mode == DeviceMode.MONITOR || !preferences.isLinked) {
            val snapshot = client.downloadSnapshot(accessToken, remote.snapshotFileId)
            repository.importStoreSnapshot(snapshot)
            repository.markStoreSnapshotSynced()
        }
        val now = System.currentTimeMillis()
        val connection = DriveConnection(
            email = email,
            storeId = remote.manifest.storeId,
            mode = mode,
            primaryDeviceId = remote.manifest.primaryDeviceId
        )
        preferences.saveConnection(connection, now)
        return DriveConnectOutcome.ExistingStore(connection, now, remote.manifest.shopName)
    }

    suspend fun initializePrimaryStore(accessToken: String): DriveSyncOutcome {
        val connection = currentConnection()
        require(connection.mode == DeviceMode.PRIMARY) { "Only the primary device can create store data." }
        val now = System.currentTimeMillis()
        val snapshot = repository.exportStoreSnapshot()
        val manifest = DriveStoreManifest(
            storeId = connection.storeId,
            primaryDeviceId = preferences.deviceId,
            shopName = snapshot.settings.shopName,
            createdAt = now,
            updatedAt = now
        )
        client.uploadStore(accessToken, manifest, snapshot)
        repository.markStoreSnapshotSynced()
        preferences.updateLastSync(now)
        return DriveSyncOutcome(now, uploaded = true, shopName = manifest.shopName)
    }

    suspend fun sync(accessToken: String): DriveSyncOutcome {
        val connection = currentConnection()
        val email = client.accountEmail(accessToken)
        require(connection.email.equals(email, ignoreCase = true)) {
            "Select the linked store Gmail ${connection.email}."
        }

        return if (connection.mode == DeviceMode.PRIMARY) {
            val currentRemote = client.findStore(accessToken)
            if (currentRemote == null) return initializePrimaryStore(accessToken)
            require(currentRemote.manifest.storeId == connection.storeId) {
                "The connected Drive contains a different store."
            }
            val now = System.currentTimeMillis()
            val snapshot = repository.exportStoreSnapshot()
            val manifest = currentRemote.manifest.copy(
                shopName = snapshot.settings.shopName,
                updatedAt = now
            )
            client.uploadStore(accessToken, manifest, snapshot)
            repository.markStoreSnapshotSynced()
            preferences.updateLastSync(now)
            DriveSyncOutcome(now, uploaded = true, shopName = manifest.shopName)
        } else {
            val remote = requireNotNull(client.findStore(accessToken)) {
                "Store data was not found in the connected Google Drive."
            }
            require(remote.manifest.storeId == connection.storeId) {
                "The connected Drive contains a different store."
            }
            val snapshot = client.downloadSnapshot(accessToken, remote.snapshotFileId)
            repository.importStoreSnapshot(snapshot)
            repository.markStoreSnapshotSynced()
            val now = System.currentTimeMillis()
            preferences.updateLastSync(now)
            DriveSyncOutcome(now, uploaded = false, shopName = remote.manifest.shopName)
        }
    }

    fun currentConnection(): DriveConnection {
        val mode = requireNotNull(preferences.mode) { "Connect the store Gmail first." }
        require(preferences.email.isNotBlank() && preferences.storeId.isNotBlank()) {
            "The Drive connection is incomplete. Reconnect the store Gmail."
        }
        return DriveConnection(
            email = preferences.email,
            storeId = preferences.storeId,
            mode = mode,
            primaryDeviceId = preferences.primaryDeviceId.ifBlank { preferences.deviceId }
        )
    }
}
