
package backendConnections

import com.wire.android.testSupport.BuildConfig
import logger.WireTestLogger
import network.BackendClient
import org.json.JSONObject
import user.utils.AccessCookie
import user.utils.AccessCredentials
import user.utils.BasicAuth
import user.utils.ClientUser
import java.net.Authenticator
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URL
import java.net.URLEncoder
import java.util.logging.Logger
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class Backend(
    val name: String,
    val backendUrl: String,
    val webappUrl: String,
    val backendWebsocket: String,
    val basicAuth: BasicAuth,
    val inbucketAuth: BasicAuth,
    val domain: String,
    val deeplink: String,
    val inbucketUrl: String,
    val inbucketUsername: String,
    val inbucketPassword: String,
    val keycloakUrl: String,
    val acmeDiscoveryUrl: String,
    val k8sNamespace: String,
    val socksProxy: String,
    var proxy: Proxy? = if (socksProxy.isNotEmpty()) {

        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(
                    "qa", BuildConfig.SOCKS_PROXY_PASSWORD_PASSWORD?.toCharArray()
                )
            }
        })
        Proxy(Proxy.Type.SOCKS, InetSocketAddress("socks.wire.link", 1080))
    } else null
) {

    fun hasInbucketSetup() = inbucketUrl.isNotEmpty()
    companion object {
        private val log: Logger = Logger.getLogger(Backend::class.simpleName)

        const val PROFILE_PICTURE_JSON_ATTRIBUTE = "complete"
        const val PROFILE_PREVIEW_PICTURE_JSON_ATTRIBUTE = "preview"

        fun getDefault():Backend?{
            return loadBackend("Staging")
        }
        fun loadBackend(connectionName: String): Backend? {

            fun field(name: String): String? =
                CredentialManager.getSecretFieldValue("BACKENDCONNECTION_$connectionName", name.uppercase())

            val backendUrl = field("backendUrl") ?: ""
            val backendWebsocket = field("backendWebsocket") ?: ""
            val basicAuthUsername = field("basicAuthUsername")
            val basicAuthPassword = field("basicAuthPassword")
            val basicAuthGeneral = field("basicAuth") ?: ""
            val inbucketUsername = field("inbucketUsername")
            val inbucketPassword = field("inbucketPassword")

            WireTestLogger.getLog("This CLasss").info("Auths are $basicAuthUsername $basicAuthPassword $basicAuthGeneral $inbucketUsername")

            return Backend(
                name = connectionName,
                backendUrl = backendUrl,
                webappUrl = field("webappUrl") ?: "",
                backendWebsocket = backendWebsocket,
                basicAuth = if (basicAuthUsername.isNullOrEmpty() || basicAuthPassword.isNullOrEmpty()) BasicAuth(basicAuthGeneral) else BasicAuth(
                    basicAuthUsername,
                    basicAuthPassword
                ),
                inbucketAuth = if (!inbucketUsername.isNullOrEmpty() && !inbucketPassword.isNullOrEmpty()) BasicAuth(
                    inbucketUsername,
                    inbucketPassword
                ) else BasicAuth(basicAuthGeneral),
                domain = field("domain") ?: "",
                deeplink = field("deeplink") ?: "",
                inbucketUrl = field("inbucketUrl") ?: "",
                inbucketUsername = inbucketUsername ?: "",
                inbucketPassword = inbucketPassword ?: "",
                keycloakUrl = field("keycloakUrl") ?: "",
                acmeDiscoveryUrl = field("acmeDiscoveryUrl") ?: "",
                k8sNamespace = field("k8sNamespace") ?: "",
                socksProxy = field("socksProxy") ?: ""
            )
        }
    }

    init {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager, TrustManager {
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = null
                override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
            }
        )
        if (!socksProxy.isNullOrEmpty()) {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(
                        "qa", BuildConfig.SOCKS_PROXY_PASSWORD_PASSWORD?.toCharArray()
                    )
                }
            })
            this.proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("socks.wire.link", 1080))
        }
        try {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
        } catch (e: Exception) {
            log.severe("Could not install all-trusting trust manager: ${e.message}")
        }
    }

    fun createPersonalUserViaBackend(user: ClientUser): ClientUser {
        WireTestLogger.getLog(Backend::class.simpleName ?: "Null").info("user is ${user}")
        val url = URL(this.backendUrl + "register")

        val requestBody = JSONObject().apply {
            put("email", user.email)
            put("name", user.name)
            put("password", user.password)
        }

        val headers = mapOf("Content-Type" to "application/json")

        val response = BackendClient.sendJsonRequestWithCookies(
            url = url,
            method = "POST",
            body = requestBody.toString(),
            headers = headers
        )

        val json = JSONObject(response.body)
        user.id = json.getString("id")
        val accessCookie = AccessCookie("zuid", response.cookies)
        user.accessCredentials = AccessCredentials(null, accessCookie)

        val activationCode = getActivationCodeForEmail(
            user.email.orEmpty()
        )
        WireTestLogger.getLog(Backend::class.simpleName ?: "Null").info("code is ${activationCode}")
        activateRegisteredEmailByBackendCode(
            user.email.orEmpty(), activationCode
        )

        return user
    }

    fun createWirelessUserViaBackend(user: ClientUser): ClientUser {
        val url = URL(this.backendUrl)

        val requestBody = JSONObject().apply {
            put("name", user.name)
        }

        if (user.expiresIn != null) {
            requestBody.put("expires_in", user.expiresIn!!.getSeconds())
        }

        val headers = mapOf("Content-Type" to "application/json")

        val response = BackendClient.sendJsonRequest(
            url = url,
            method = "POST",
            body = requestBody.toString(),
            headers = headers
        )

        val connection = url.openConnection() as HttpURLConnection
        val cookiesHeader = connection.getHeaderField("Set-Cookie")
        val cookies = HttpCookie.parse(cookiesHeader)

        val json = JSONObject(response)
        user.id = json.getString("id")
        val accessCookie = AccessCookie("zuid", cookies)
        user.accessCredentials = AccessCredentials(null, accessCookie)

        val activationCode = getActivationCodeForEmail(
            user.email.orEmpty()
        )
        WireTestLogger.getLog(Backend::class.simpleName ?: "Null").info("code is ${activationCode}")

        activateRegisteredEmailByBackendCode(user.email.orEmpty(), activationCode)

        return user
    }


    fun getActivationCodeForEmail(email: String): String {
        val encodedEmail = URLEncoder.encode(email, "UTF-8")
        val url = URL("${backendUrl}i/users/activation-code?email=$encodedEmail")

        val headers = mapOf(
            "Authorization" to basicAuth.getEncoded(),
            "Accept" to "application/json"
        )

        val response = BackendClient.sendJsonRequest(
            url = url,
            method = "GET",
            body = null,
            headers = headers
        )

        return JSONObject(response).getString("code")
    }

    fun trigger2FA(email: String) {
        val url = URL("${backendUrl}v5/verification-code/send")

        val requestBody = JSONObject().apply {
            put("action", "login")
            put("email", email)
        }

        val headers = mapOf(
            "Authorization" to basicAuth.getEncoded(),
            "Content-Type" to "application/json",
            "Accept" to "application/json"
        )

        try {
            BackendClient.sendJsonRequest(
                url = url,
                method = "POST",
                body = requestBody.toString(),
                headers = headers
            )
        } catch (e: Exception) {
            // Optional: Allow HTTP 429 as acceptable like in Java

        }
    }


    //Used to active a register user email
    fun activateRegisteredEmailByBackendCode(email: String, code: String): String {

        val url = URL("${backendUrl}activate")

        val requestBody = JSONObject().apply {
            put("email", email)
            put("code", code)
            put("dryrun", false)
        }
        WireTestLogger.getLog(Backend::class.simpleName ?: "Null").info("JsonBody is ${requestBody}")

        val headers = mapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json"
        )

        val response = BackendClient.sendJsonRequest(
            url = url,
            method = "POST",
            body = requestBody.toString(),
            headers = headers
        )
        WireTestLogger.getLog(Backend::class.simpleName ?: "Null").info("JsonBody response is ${requestBody}")

        return "Email Registered"
    }

    fun String.composeCompleteUrl(): String {
        return "${backendUrl}$this"
    }
}
