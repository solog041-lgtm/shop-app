package com.example.momosshopmanager.data

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object SyncManager {
    var databaseUrl: String = "https://shop-5b949-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /**
     * Performs a generic HTTP request.
     */
    private suspend fun makeHttpRequest(
        urlString: String,
        method: String,
        body: String? = null
    ): String? = withContext(Dispatchers.IO) {
        if (databaseUrl.isBlank()) return@withContext null
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            if (body != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
                connection.doOutput = true
                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { reader ->
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    return@withContext response.toString()
                }
            } else {
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Reads google-services.json from assets, extracts project_id,
     * and pings standard database region URLs to detect the active one.
     */
    suspend fun detectAndVerifyDatabaseUrl(context: android.content.Context): String? = withContext(Dispatchers.IO) {
        val projectId = try {
            val jsonString = context.assets.open("google-services.json").bufferedReader().use { it.readText() }
            val element = Json.parseToJsonElement(jsonString).jsonObject
            val projectInfo = element["project_info"]?.jsonObject ?: return@withContext null
            
            val configUrl = projectInfo["firebase_url"]?.jsonPrimitive?.content
            if (!configUrl.isNullOrBlank()) {
                return@withContext configUrl
            }
            
            projectInfo["project_id"]?.jsonPrimitive?.content ?: return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }

        // Try standard regions
        val regions = listOf(
            "https://$projectId-default-rtdb.asia-southeast1.firebasedatabase.app",
            "https://$projectId-default-rtdb.firebaseio.com",
            "https://$projectId-default-rtdb.europe-west1.firebasedatabase.app"
        )
        
        for (url in regions) {
            var connection: HttpURLConnection? = null
            try {
                val testUrl = URL("$url/.json")
                connection = testUrl.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                val code = connection.responseCode
                // A valid Firebase RTDB database (even if empty) returns 200 OK with body "null"
                if (code in 200..299) {
                    return@withContext url
                }
            } catch (e: Exception) {
                // ignore and try next
            } finally {
                connection?.disconnect()
            }
        }
        
        // Fallback to Singapore
        "https://$projectId-default-rtdb.asia-southeast1.firebasedatabase.app"
    }

    // --- Sales Sync ---

    suspend fun pushSale(syncCode: String, sale: Sale): Boolean {
        if (syncCode.isBlank() || databaseUrl.isBlank()) return false
        val url = "$databaseUrl/shops/$syncCode/sales/${sale.id}.json"
        val body = json.encodeToString(sale)
        return makeHttpRequest(url, "PUT", body) != null
    }

    suspend fun deleteSale(syncCode: String, saleId: String): Boolean {
        if (syncCode.isBlank() || databaseUrl.isBlank()) return false
        val url = "$databaseUrl/shops/$syncCode/sales/$saleId.json"
        return makeHttpRequest(url, "DELETE") != null
    }

    suspend fun pullSales(syncCode: String): List<Sale> {
        if (syncCode.isBlank() || databaseUrl.isBlank()) return emptyList()
        val url = "$databaseUrl/shops/$syncCode/sales.json"
        val response = makeHttpRequest(url, "GET") ?: return emptyList()
        if (response.trim() == "null" || response.trim().isEmpty()) return emptyList()

        return try {
            val salesMap = json.decodeFromString<Map<String, Sale>>(response)
            salesMap.values.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- Menu Sync ---

    suspend fun pushMenu(syncCode: String, menu: List<MenuItem>): Boolean {
        if (syncCode.isBlank() || databaseUrl.isBlank()) return false
        val url = "$databaseUrl/shops/$syncCode/menu.json"
        val body = json.encodeToString(menu)
        return makeHttpRequest(url, "PUT", body) != null
    }

    suspend fun pullMenu(syncCode: String): List<MenuItem>? {
        if (syncCode.isBlank() || databaseUrl.isBlank()) return null
        val url = "$databaseUrl/shops/$syncCode/menu.json"
        val response = makeHttpRequest(url, "GET") ?: return null
        if (response.trim() == "null" || response.trim().isEmpty()) return null

        return try {
            json.decodeFromString<List<MenuItem>>(response)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- Device Registry ---

    suspend fun registerDevice(
        syncCode: String,
        phoneNumber: String,
        role: UserRole,
        pin: String
    ): Boolean {
        if (syncCode.isBlank() || phoneNumber.isBlank() || databaseUrl.isBlank()) return false
        val sanitizedPhone = phoneNumber.filter { it.isDigit() }
        val url = "$databaseUrl/shops/$syncCode/devices/$sanitizedPhone.json"
        val deviceData = DeviceInfo(
            phoneNumber = sanitizedPhone,
            role = role,
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            registeredAt = System.currentTimeMillis(),
            pin = pin
        )
        val body = json.encodeToString(deviceData)
        return makeHttpRequest(url, "PUT", body) != null
    }

    suspend fun pullDevices(syncCode: String): List<DeviceInfo> {
        if (syncCode.isBlank() || databaseUrl.isBlank()) return emptyList()
        val url = "$databaseUrl/shops/$syncCode/devices.json"
        val response = makeHttpRequest(url, "GET") ?: return emptyList()
        if (response.trim() == "null" || response.trim().isEmpty()) return emptyList()

        return try {
            val devicesMap = json.decodeFromString<Map<String, DeviceInfo>>(response)
            devicesMap.values.toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun verifyCredentials(syncCode: String, phoneNumber: String, pin: String): DeviceInfo? {
        if (syncCode.isBlank() || phoneNumber.isBlank() || databaseUrl.isBlank()) return null
        val sanitizedPhone = phoneNumber.filter { it.isDigit() }
        val url = "$databaseUrl/shops/$syncCode/devices/$sanitizedPhone.json"
        val response = makeHttpRequest(url, "GET") ?: return null
        if (response.trim() == "null" || response.trim().isEmpty()) return null

        return try {
            val device = json.decodeFromString<DeviceInfo>(response)
            if (device.pin == pin) device else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
