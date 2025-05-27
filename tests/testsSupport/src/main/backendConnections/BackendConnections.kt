//package backendConnections
//
//import java.util.logging.Logger
//import user.utils.BasicAuth
//import java.net.*;
//import javax.net.ssl.TrustManager
//import androidx.test.platform.app.InstrumentationRegistry
//import com.wire.android.testSupport.BuildConfig
//import javax.net.ssl.X509TrustManager
//import javax.net.ssl.*
//
//class Backend(
//    val name: String,
//    val backendUrl: String,
//    val webappUrl: String,
//    val backendWebsocket: String,
//    val basicAuth: BasicAuth,
//    val inbucketAuth: BasicAuth,
//    val domain: String,
//    val deeplink: String,
//    val inbucketUrl: String,
//    val keycloakUrl: String,
//    val acmeDiscoveryUrl: String,
//    val k8sNamespace: String,
//    val socksProxy: String,
//    var proxy: Proxy? = if (!socksProxy.isNullOrEmpty()) {
//        Authenticator.setDefault(object : Authenticator() {
//            override fun getPasswordAuthentication(): PasswordAuthentication {
//                return PasswordAuthentication(
//                    "qa", BuildConfig.SOCKS_PROXY_PASSWORD_PASSWORD?.toCharArray()
//                )
//            }
//        })
//        Proxy(Proxy.Type.SOCKS, InetSocketAddress("socks.wire.link", 1080))
//    } else null
//) {
//    companion object {
//        private val log: Logger = Logger.getLogger(Backend::class.simpleName)
//
//        const val PROFILE_PICTURE_JSON_ATTRIBUTE = "complete"
//        const val PROFILE_PREVIEW_PICTURE_JSON_ATTRIBUTE = "preview"
//
//        fun loadBackend(connectionName: String): Backend? {
//            fun field(name: String): String? =
//                CredentialManager.getSecretFieldValue("BACKENDCONNECTION_$connectionName", name.uppercase())
//
//            val backendUrl = field("backendUrl") ?: return null
//            val backendWebsocket = field("backendWebsocket") ?: return null
//            val basicAuthUsername = field("basicAuthUsername") ?: return null
//            val basicAuthPassword = field("basicAuthPassword") ?: return null
//            val inbucketUsername = field("inbucketUsername") ?: basicAuthUsername
//            val inbucketPassword = field("inbucketPassword") ?: basicAuthPassword
//
//            return Backend(
//                name = connectionName,
//                backendUrl = backendUrl,
//                webappUrl = field("webappUrl") ?: "",
//                backendWebsocket = backendWebsocket,
//                basicAuth = BasicAuth(basicAuthUsername, basicAuthPassword),
//                inbucketAuth = BasicAuth(inbucketUsername, inbucketPassword),
//                domain = field("domain") ?: "",
//                deeplink = field("deeplink") ?: "",
//                inbucketUrl = field("inbucketUrl") ?: "",
//                keycloakUrl = field("keycloakUrl") ?: "",
//                acmeDiscoveryUrl = field("acmeDiscoveryUrl") ?: "",
//                k8sNamespace = field("k8sNamespace") ?: "",
//                socksProxy = field("socksProxy") ?: ""
//            )
//        }
//    }
//
//
//    init {
//        // Install the all-trusting trust manager
//        val trustAllCerts = arrayOf<TrustManager>(
//            object : X509TrustManager, TrustManager {
//                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = null
//                override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
//                override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
//            }
//        )
//        if (!socksProxy.isNullOrEmpty()) {
//            Authenticator.setDefault(object : Authenticator() {
//                override fun getPasswordAuthentication(): PasswordAuthentication {
//                    return PasswordAuthentication(
//                        "qa", BuildConfig.SOCKS_PROXY_PASSWORD_PASSWORD?.toCharArray()
//                    )
//                }
//            })
//            this.proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("socks.wire.link", 1080))
//        }
//        try {
//            val sslContext = SSLContext.getInstance("SSL")
//            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
//            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
//        } catch (e: Exception) {
//            log.severe("Could not install all-trusting trust manager: ${e.message}")
//        }
//    }
//
//}
