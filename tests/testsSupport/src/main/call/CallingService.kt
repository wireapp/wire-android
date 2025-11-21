@file:Suppress(
    "TooGenericExceptionCaught",
    "LargeClass",
    "LongParameterList",
    "NestedBlockDepth",
    "MagicNumber",
    "TooManyFunctions",
    "TooGenericExceptionThrown"
)
package call
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import call.models.Call
import call.models.CallFlow
import call.models.CallRequest
import call.models.Instance
import call.models.InstanceRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wire.android.testSupport.BuildConfig
import logger.WireTestLogger
import network.HttpRequestException
import java.io.IOException
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class CallingService(
    private val callingServiceAddress: String
) {

    val applicationJson = "application/json"
    val contentTypeString = "Content-Type"

    private val gson = Gson()
    private val cookies = ConcurrentHashMap<String, HttpCookie>()
    private var basicAuthUser: String? = null
    private var basicAuthPassword: String? = null

    private fun getBasicAuthentication(): String {
        if (basicAuthUser == null) basicAuthUser = "qa"
        if (basicAuthPassword == null) {
            basicAuthPassword = BuildConfig.CALLINGSERVICE_BASIC_AUTH_PASSWORD
        }
        val auth = "$basicAuthUser:$basicAuthPassword"
        val encodedAuth = Base64.encodeToString(auth.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        return "Basic $encodedAuth"
    }

    private fun getUrl(path: String): URL {
        return try {
            URI(callingServiceAddress + path).toURL()
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }
    }

    private fun buildRequestForInstance(
        instanceId: String?,
        path: String,
        contentType: String? = applicationJson,
        accept: String? = applicationJson
    ): HttpURLConnection {
        val url = getUrl(path)
        val conn = (url.openConnection() as HttpURLConnection)
        conn.setRequestProperty("Authorization", getBasicAuthentication())
        contentType?.let { conn.setRequestProperty(contentTypeString, it) }
        accept?.let { conn.setRequestProperty("Accept", it) }
        cookies[instanceId]?.let {
            if (it.name == "SERVERID") conn.setRequestProperty("Cookie", "${it.name}=${it.value}")
        }
        return conn
    }

    private fun buildRequestForCertainInstance(
        serverId: String,
        path: String,
        contentType: String? = applicationJson,
        accept: String? = applicationJson
    ): HttpURLConnection {
        val url = getUrl(path)
        val conn = (url.openConnection() as HttpURLConnection)
        conn.setRequestProperty("Authorization", getBasicAuthentication())
        contentType?.let { conn.setRequestProperty(contentTypeString, it) }
        accept?.let { conn.setRequestProperty("Accept", it) }
        conn.setRequestProperty("Cookie", "SERVERID=$serverId")
        return conn
    }


    fun createInstance(request: InstanceRequest): Instance {
        return try {
            val url = getUrl("/api/v1/instance/create")
            val conn = (url.openConnection() as HttpURLConnection)
            conn.setRequestProperty("Authorization", getBasicAuthentication())
            conn.setRequestProperty(contentTypeString, applicationJson)
            conn.setRequestProperty("Accept", applicationJson)

            val response = httpPost(conn, gson.toJson(request), intArrayOf(HttpURLConnection.HTTP_OK))
            val instance = gson.fromJson(response, Instance::class.java)

            conn.getHeaderField("Set-Cookie")?.let { cookieHeader ->
                HttpCookie.parse(cookieHeader)
                    .firstOrNull { it.name == "SERVERID" }
                    ?.let { cookies[instance.id!!] = it }
            }
            instance
        } catch (ex: Exception) {
            throw HttpRequestException("createInstance failed: ${ex.message}", 500)
        }
    }

    fun destroyInstance(instance: Instance): Instance {
        val target = "/api/v1/instance/${instance.id}/destroy"
        val conn = buildRequestForInstance(instance.id, target)
        return try {
            val response = httpPut(conn, "", intArrayOf(HttpURLConnection.HTTP_OK))
            cookies.remove(instance.id)
            gson.fromJson(response, Instance::class.java)
        } catch (ex: Exception) {
            throw HttpRequestException("destroyInstance failed: ${ex.message}", 500)
        }
    }

    fun getInstance(instance: Instance): Instance {
        val target = "/api/v1/instance/${instance.id}/status"
        val conn = buildRequestForInstance(instance.id, target)
        return try {
            val response = httpGet(conn, intArrayOf(HttpURLConnection.HTTP_OK))
            gson.fromJson(response, Instance::class.java)
        } catch (ex: Exception) {
            throw HttpRequestException("getInstance failed: ${ex.message}", 500)
        }
    }

    fun getFlows(instance: Instance): List<CallFlow> {
        val target = "/api/v1/instance/${instance.id}/flows"
        val conn = buildRequestForInstance(instance.id, target)
        return try {
            val response = httpGet(conn, intArrayOf(HttpURLConnection.HTTP_OK))
            val type = object : TypeToken<List<CallFlow>>() {}.type
            gson.fromJson(response, type)
        } catch (ex: Exception) {
            throw HttpRequestException("getFlows failed: ${ex.message}", 500)
        }
    }

    fun getPackets(instance: Instance): String {
        val target = "/api/v1/instance/${instance.id}/packets"
        val conn = buildRequestForInstance(instance.id, target)
        return try {
            httpGet(conn, intArrayOf(HttpURLConnection.HTTP_OK))
        } catch (ex: Exception) {
            throw HttpRequestException("getPackets failed: ${ex.message}", 500)
        }
    }

    fun getScreenshot(instance: Instance): Bitmap? {
        val target = "/api/v1/instance/${instance.id}/screenshot"
        val conn = buildRequestForInstance(instance.id, target, null, null)
        return try {
            conn.requestMethod = "GET"
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw HttpRequestException("Screenshot failed", code)
            BitmapFactory.decodeStream(conn.inputStream)
        } catch (e: IOException) {
            Log.e("CallingService", "getScreenshot failed", e)
            null
        } finally {
            conn.disconnect()
        }
    }

    fun getLog(instance: Instance): String {
        val target = "/api/v1/instance/${instance.id}/log"
        val conn = buildRequestForInstance(instance.id, target, "text/plain", "text/plain")
        return httpGet(conn, intArrayOf(HttpURLConnection.HTTP_OK, 404))
    }

    fun getAllRunningInstances(): List<Instance> {
        val target = "/api/v1/instance"
        val serverIds = listOf("c1", "c2", "c3", "c4")
        val instances = mutableListOf<Instance>()

        for (serverId in serverIds) {
            val conn = buildRequestForCertainInstance(serverId, target)
            try {
                val response = httpGet(conn, intArrayOf(HttpURLConnection.HTTP_OK))
                val type = object : TypeToken<List<Instance>>() {}.type
                val serverInstances: List<Instance> = gson.fromJson(response, type)
                serverInstances.forEach {
                    if (!cookies.containsKey(it.id)) {
                        cookies[it.id!!] = HttpCookie("SERVERID", serverId)
                    }
                }
                instances.addAll(serverInstances)
            } catch (ex: Exception) {
                throw HttpRequestException("getAllRunningInstances failed: ${ex.message}", 500)
            }
        }

        return instances
    }

    // endregion

    // region CALL APIs

    fun start(instance: Instance, request: CallRequest): Call =
        performCall(instance, "/call/start", request)

    fun startVideo(instance: Instance, request: CallRequest): Call =
        performCall(instance, "/call/startVideo", request)

    fun acceptNext(instance: Instance, request: CallRequest): Call =
        performCall(instance, "/call/acceptNext", request)

    fun acceptNextVideo(instance: Instance, request: CallRequest): Call =
        performCall(instance, "/call/acceptNextVideo", request)

    fun stop(instance: Instance, call: Call): Call =
        performCallPut(instance, "/call/${call.id}/stop")

    fun decline(instance: Instance, request: CallRequest): Call =
        performCall(instance, "/call/decline", request)

    fun getCall(instance: Instance, call: Call): Call =
        performCallGet(instance, "/call/${call.id}/status")

    fun switchVideoOn(instance: Instance, call: Call): Call =
        performCallPut(instance, "/call/${call.id}/switchVideoOn")

    fun switchVideoOff(instance: Instance, call: Call): Call =
        performCallPut(instance, "/call/${call.id}/switchVideoOff")

    fun pauseVideoCall(instance: Instance, call: Call): Call =
        performCallPut(instance, "/call/${call.id}/pauseVideoCall")

    fun unpauseVideoCall(instance: Instance, call: Call): Call =
        performCallPut(instance, "/call/${call.id}/unpauseVideoCall")

    fun switchScreensharingOn(instance: Instance, call: Call): Call =
        performCallPut(instance, "/call/${call.id}/switchScreensharingOn")

    fun switchScreensharingOff(instance: Instance, call: Call): Call =
        performCallPut(instance, "/call/${call.id}/switchScreensharingOff")

    fun muteMicrophone(instance: Instance, call: Call): Call =
        performCallPut(instance, "/call/${call.id}/mute")

    fun unmuteMicrophone(instance: Instance, call: Call): Call =
        performCallPut(instance, "/call/${call.id}/unmute")

    fun maximiseCall(instance: Instance, call: Call): Call =
        performCallPut(instance, "/call/${call.id}/maximiseVideoCall")

    // moderation helpers
    fun muteParticipant(instance: Instance, call: Call, name: String): Call {
        val encoded = name.replace(" ", "%20")
        return performCallPut(instance, "/call/${call.id}/muteParticipant/$encoded")
    }

    fun muteAllOthers(instance: Instance, call: Call, name: String): Call {
        val encoded = name.replace(" ", "%20")
        return performCallPut(instance, "/call/${call.id}/muteAllOthers/$encoded")
    }

    // endregion

    // region CALL HELPERS

    private fun performCall(instance: Instance, path: String, request: Any): Call {
        val conn = buildRequestForInstance(instance.id, "/api/v1/instance/${instance.id}$path")
        val response = httpPost(conn, gson.toJson(request), intArrayOf(HttpURLConnection.HTTP_OK))
        return gson.fromJson(response, Call::class.java)
    }

    private fun performCallPut(instance: Instance, path: String): Call {
        val conn = buildRequestForInstance(instance.id, "/api/v1/instance/${instance.id}$path")
        val response = httpPut(conn, "", intArrayOf(HttpURLConnection.HTTP_OK))
        return gson.fromJson(response, Call::class.java)
    }

    private fun performCallGet(instance: Instance, path: String): Call {
        val conn = buildRequestForInstance(instance.id, "/api/v1/instance/${instance.id}$path")
        val response = httpGet(conn, intArrayOf(HttpURLConnection.HTTP_OK))
        return gson.fromJson(response, Call::class.java)
    }

    // endregion

    // region HTTP HELPERS

    private fun httpGet(conn: HttpURLConnection, acceptableCodes: IntArray): String {
        return try {
            conn.requestMethod = "GET"
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            assertResponseCode(code, acceptableCodes)
            body
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPost(conn: HttpURLConnection, body: String, acceptableCodes: IntArray): String {
        return try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            WireTestLogger.getLog("Api response").info(response)
            assertResponseCode(code, acceptableCodes)
            response
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPut(conn: HttpURLConnection, body: String, acceptableCodes: IntArray): String {
        return try {
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            assertResponseCode(code, acceptableCodes)
            response
        } finally {
            conn.disconnect()
        }
    }

    private fun assertResponseCode(code: Int, acceptable: IntArray) {
        if (!acceptable.contains(code)) {
            throw HttpRequestException("Unexpected response code: $code (expected ${acceptable.joinToString()})", code)
        }
    }

    // endregion
}
