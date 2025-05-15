package com.wire.android.tests.core.utils

import com.wire.android.tests.core.config.Credentials
import com.wire.android.tests.core.exceptions.HttpRequestException
import com.wire.android.tests.core.services.RESTResponseHandler
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.logging.Logger
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.MediaType

class OktaAPIClient private constructor() {

    companion object {
        private val log: Logger = Logger.getLogger(OktaAPIClient::class.simpleName)
        private const val BASE_URI = "https://dev-500508-admin.oktapreview.com"
        private const val CONNECT_TIMEOUT_SECONDS = 3L
        private const val READ_TIMEOUT_SECONDS = 240L

        private val restHandlers = CommonRESTHandlers(
            object : RESTResponseHandler {
                override fun verifyRequestResult(
                    currentResponseCode: Int,
                    acceptableResponseCodes: IntArray?,
                    message: String?
                ) {
                    if (acceptableResponseCodes?.any { it == currentResponseCode } == true) return
                    throw HttpRequestException(
                        "Request failed with code: $currentResponseCode. " +
                                "Expected: ${acceptableResponseCodes?.contentToString()}. " +
                                "Message: ${message ?: "No details provided"}",
                        currentResponseCode
                    )
                }
            },
            maxRetries = 3
        )

        private var apiKey: String? = null

        fun getInstance(): OktaAPIClient {
            return OktaAPIClient().apply {
                // Initialize API key on first use
                getAPIKey()
            }
        }
    }

    private var applicationId: String? = null
    private val userIds = mutableSetOf<String>()
    private val client: Client = createHttpClient()

    private fun createHttpClient(): Client {
        val config = ClientConfig().apply {
            property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT_SECONDS * 1000)
            property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT_SECONDS * 1000)
        }
        return ClientBuilder.newClient(config)
    }

    private fun getAPIKey(): String {
        return apiKey ?: Credentials().getCredentials("OKTA_API_KEY").also { apiKey = it }
    }

    private fun buildRequest(path: String, acceptType: MediaType = MediaType.APPLICATION_JSON_TYPE): Invocation.Builder {
        val target = "$BASE_URI/${path.removePrefix("/")}"
        log.info("Building request to: $target")
        return client.target(target).request()
            .accept(acceptType)
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .header("Authorization", "SSWS ${getAPIKey()}")
    }

    fun createApplication(label: String, finalizeUrl: String): String {
        val resource = Thread.currentThread().contextClassLoader
            ?.getResourceAsStream("okta/appCreation.json")
            ?: throw IllegalStateException("Could not load app creation template")

        val template = resource.use { Scanner(it).useDelimiter("\\A").next() }
        val requestBody = JSONObject(template).apply {
            put("label", label)
            getJSONObject("settings").getJSONObject("signOn").apply {
                put("ssoAcsUrl", finalizeUrl)
                put("audience", finalizeUrl)
                put("recipient", finalizeUrl)
                put("destination", finalizeUrl)
            }
        }

        val response = restHandlers.httpPost(
            buildRequest("api/v1/apps"),
            requestBody.toString(),
            intArrayOf(200))

            return JSONObject(response).getString("id").also {
                applicationId = it
                assignApplicationToGroup(it, fetchGroupId("Everyone"))
            }
    }

    fun createWronglyConfiguredApplication(label: String, backendUrl: String) {
        val finalizeUrl = "${backendUrl.substringBeforeLast("/")}/sso/finalize-login"
        val requestBody = JSONObject(
            Thread.currentThread().contextClassLoader
                ?.getResourceAsStream("okta/appCreation.json")
                .use { Scanner(it).useDelimiter("\\A").next() }
        ).apply {
            put("label", "Wrongly configured application $label")
            getJSONObject("settings").getJSONObject("signOn").apply {
                put("ssoAcsUrl", finalizeUrl)
                put("audience", finalizeUrl)
                put("recipient", finalizeUrl)
                put("destination", "https://wrong-destination.local/")
            }
        }

        applicationId = JSONObject(
            restHandlers.httpPost(
                buildRequest("api/v1/apps"),
                requestBody.toString(),
                intArrayOf(200)
            )
        ).getString("id").also {
            assignApplicationToGroup(it, fetchGroupId("Everyone"))
        }
    }

    private fun JSONArray.asSequence(): Sequence<JSONObject> =
        (0 until this.length()).asSequence().map { this.getJSONObject(it) }

    fun getApplicationId(label: String): String? {
        val webResource = buildRequest("api/v1/apps")
        val output = restHandlers.httpGet(webResource, intArrayOf(200))
        return JSONArray(output).asSequence()
            .firstOrNull { it.getString("label") == label }
            ?.getString("id")
    }


    private fun fetchGroupId(groupName: String): String {
        val response = restHandlers.httpGet(
            buildRequest("api/v1/groups?limit=100"),
            intArrayOf(200))

        return JSONArray(response).asSequence()
            .firstOrNull { it.getJSONObject("profile").getString("name") == groupName }
            ?.getString("id")
            ?: throw IllegalStateException("Group '$groupName' not found")
    }

    private fun assignApplicationToGroup(appId: String, groupId: String) {
        restHandlers.httpPut(
            buildRequest("api/v1/apps/$appId/groups/$groupId"),
            "{}",
            intArrayOf(200))
    }

    fun getApplicationMetadata(): String {
        return restHandlers.httpGet(
            buildRequest("api/v1/apps/$applicationId/sso/saml/metadata", MediaType.APPLICATION_XML_TYPE),
            intArrayOf(200))
    }

    fun createUser(name: String, email: String, password: String): String {
        val requestBody = JSONObject().apply {
            put("profile", JSONObject().apply {
                put("firstName", name)
                put("lastName", name)
                put("email", email)
                put("login", email)
            })
            put("credentials", JSONObject().apply {
                put("password", JSONObject().apply { put("value", password) })
                put("recovery_question", JSONObject().apply {
                    put("question", "What is the answer to life, the universe and everything?")
                    put("answer", "fortytwo")
                })
            })
        }

        val response = restHandlers.httpPost(
            buildRequest("api/v1/users?activate=true"),
            requestBody.toString(),
            intArrayOf(200))

        return JSONObject(response).getString("id").also {
            userIds.add(it)
        }
    }

    fun deleteUser(userId: String) {
        try {
            restHandlers.httpPost(
                buildRequest("api/v1/users/$userId/lifecycle/deactivate"),
                "",
                intArrayOf(200))

            restHandlers.httpDelete(
                buildRequest("api/v1/users/$userId"),
                intArrayOf(204))

            userIds.remove(userId)
        } catch (e: Exception) {
            log.warning("Failed to delete user $userId: ${e.message}")
        }
    }

    fun cleanUp() {
        applicationId?.let { appId ->
            try {
                restHandlers.httpPost(
                    buildRequest("api/v1/apps/$appId/lifecycle/deactivate"),
                    "",
                    intArrayOf(200))

                restHandlers.httpDelete(
                    buildRequest("api/v1/apps/$appId"),
                    intArrayOf(204))
            } catch (e: Exception) {
                log.warning("Failed to clean up application $appId: ${e.message}")
            } finally {
                applicationId = null
            }
        }

        userIds.toList().forEach { userId ->
            try {
                deleteUser(userId)
            } catch (e: Exception) {
                log.warning("Failed to delete user $userId during cleanup: ${e.message}")
            }
        }
    }
}
