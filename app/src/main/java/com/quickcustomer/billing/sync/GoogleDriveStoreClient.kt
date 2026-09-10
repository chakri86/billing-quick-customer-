package com.quickcustomer.billing.sync

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

class GoogleDriveStoreClient {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun accountEmail(accessToken: String): String = withContext(Dispatchers.IO) {
        val body = request(
            url = "https://www.googleapis.com/oauth2/v3/userinfo",
            accessToken = accessToken
        )
        JSONObject(body).optString("email").trim().ifBlank {
            throw IOException("Google did not return the selected account email.")
        }
    }

    suspend fun findStore(accessToken: String): DriveStoreFiles? = withContext(Dispatchers.IO) {
        val manifestId = findFileId(accessToken, MANIFEST_FILE_NAME) ?: return@withContext null
        val snapshotId = findFileId(accessToken, SNAPSHOT_FILE_NAME)
            ?: throw IOException("The store manifest exists, but its data snapshot is missing.")
        val manifest = json.decodeFromString<DriveStoreManifest>(download(accessToken, manifestId))
        require(manifest.formatVersion <= DriveStoreManifest.CURRENT_FORMAT) {
            "This store was created by a newer Quick Customer version. Update the application first."
        }
        DriveStoreFiles(manifestId, manifest, snapshotId)
    }

    suspend fun downloadSnapshot(accessToken: String, fileId: String): StoreSnapshot =
        withContext(Dispatchers.IO) {
            json.decodeFromString<StoreSnapshot>(download(accessToken, fileId))
        }

    suspend fun uploadStore(
        accessToken: String,
        manifest: DriveStoreManifest,
        snapshot: StoreSnapshot
    ): DriveStoreFiles = withContext(Dispatchers.IO) {
        val snapshotJson = json.encodeToString(snapshot)
        val snapshotId = upsertJson(accessToken, SNAPSHOT_FILE_NAME, snapshotJson)
        val manifestJson = json.encodeToString(manifest)
        val manifestId = upsertJson(accessToken, MANIFEST_FILE_NAME, manifestJson)
        DriveStoreFiles(manifestId, manifest, snapshotId)
    }

    private fun findFileId(accessToken: String, name: String): String? {
        val escapedName = name.replace("'", "\\'")
        val query = "name='$escapedName' and trashed=false"
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        val url = "https://www.googleapis.com/drive/v3/files" +
            "?spaces=appDataFolder&q=$encodedQuery&pageSize=2&fields=files(id,name)"
        val response = JSONObject(request(url, accessToken))
        val files = response.optJSONArray("files") ?: return null
        return if (files.length() == 0) null else files.getJSONObject(0).getString("id")
    }

    private fun download(accessToken: String, fileId: String): String = request(
        url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media",
        accessToken = accessToken
    )

    private fun upsertJson(accessToken: String, name: String, content: String): String {
        val existingId = findFileId(accessToken, name)
        if (existingId != null) {
            request(
                url = "https://www.googleapis.com/upload/drive/v3/files/$existingId?uploadType=media&fields=id",
                accessToken = accessToken,
                method = "PATCH",
                contentType = JSON_CONTENT_TYPE,
                body = content.toByteArray(StandardCharsets.UTF_8)
            )
            return existingId
        }

        val boundary = "quick-customer-${System.currentTimeMillis()}"
        val metadata = JSONObject()
            .put("name", name)
            .put("parents", org.json.JSONArray().put("appDataFolder"))
            .put("mimeType", JSON_MIME_TYPE)
            .toString()
        val multipart = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: $JSON_CONTENT_TYPE\r\n\r\n")
            append(content)
            append("\r\n--$boundary--\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        val response = request(
            url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id",
            accessToken = accessToken,
            method = "POST",
            contentType = "multipart/related; boundary=$boundary",
            body = multipart
        )
        return JSONObject(response).getString("id")
    }

    private fun request(
        url: String,
        accessToken: String,
        method: String = "GET",
        contentType: String? = null,
        body: ByteArray? = null
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            if (contentType != null) connection.setRequestProperty("Content-Type", contentType)
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body) }
            }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                throw IOException("Google Drive request failed ($responseCode). Please reconnect and try again.")
            }
            response
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email"
        const val OPENID_SCOPE = "openid"
        private const val MANIFEST_FILE_NAME = "quick-customer-store.json"
        private const val SNAPSHOT_FILE_NAME = "quick-customer-snapshot.json"
        private const val JSON_MIME_TYPE = "application/json"
        private const val JSON_CONTENT_TYPE = "application/json; charset=UTF-8"
    }
}
