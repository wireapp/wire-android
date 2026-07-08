/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
@file:Suppress(
    "TooGenericExceptionCaught",
    "LargeClass",
    "PackageNaming",
    "TooGenericExceptionThrown"
)
package backendUtils

import CredentialsManager
import com.wire.android.testSupport.BuildConfig
import network.NetworkBackendClient
import user.utils.BasicAuth
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy

@Suppress("LongParameterList", "MagicNumber", "TooManyFunctions")
class BackendClient(
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
    var proxy: Proxy? = null
) {

    fun hasInbucketSetup() = inbucketUrl.isNotEmpty()

    companion object {
        const val contentType = "Content-Type"
        const val accept = "Accept"
        const val applicationJson = "application/json"
        const val AUTHORIZATION = "Authorization"
        private const val DEFAULT_BACKEND_NAME = "STAGING"

        data class BackendSecrets(
            val backendUrl: String,
            val backendWebsocket: String,
            val webappUrl: String,
            val basicAuthUsername: String?,
            val basicAuthPassword: String?,
            val basicAuthGeneral: String,
            val inbucketUsername: String?,
            val inbucketPassword: String?,
            val domain: String,
            val deeplink: String,
            val inbucketUrl: String,
            val keycloakUrl: String,
            val acmeDiscoveryUrl: String,
            val k8sNamespace: String,
            val socksProxy: String
        )

        private fun loadSecrets(connectionName: String): BackendSecrets {
            val parentKey = buildConfigParentKey(connectionName)
            fun field(name: String): String? =
                CredentialsManager.getSecretFieldValue(parentKey, name.toBuildConfigKeySegment())

            return BackendSecrets(
                backendUrl = field("backendUrl") ?: "",
                backendWebsocket = field("backendWebsocket") ?: "",
                webappUrl = field("webappUrl") ?: "",
                basicAuthUsername = field("basicAuthUsername"),
                basicAuthPassword = field("basicAuthPassword"),
                basicAuthGeneral = field("basicAuth") ?: "",
                inbucketUsername = field("inbucketUsername"),
                inbucketPassword = field("inbucketPassword"),
                domain = field("domain") ?: "",
                deeplink = field("deeplink") ?: "",
                inbucketUrl = field("inbucketUrl") ?: "",
                keycloakUrl = field("keycloakUrl") ?: "",
                acmeDiscoveryUrl = field("acmeDiscoveryUrl") ?: "",
                k8sNamespace = field("k8sNamespace") ?: "",
                socksProxy = field("socksProxy") ?: ""
            )
        }

        // Backend names can contain hyphens, while generated BuildConfig keys use underscores.
        private fun String.toBuildConfigKeySegment(): String =
            replace("[.\\-\\s]+".toRegex(), "_").uppercase()

        private fun buildConfigParentKey(connectionName: String): String =
            "BACKENDCONNECTION_${connectionName.toBuildConfigKeySegment()}"

        private fun validateRequiredSecrets(connectionName: String, secrets: BackendSecrets) {
            if (secrets.backendUrl.isNotBlank()) return

            throw IllegalStateException(
                "Backend '$connectionName' is missing required field 'backendUrl'. " +
                        "Expected generated BuildConfig prefix '${buildConfigParentKey(connectionName)}'. " +
                        "Check that secrets.json contains a matching BackendConnection item and rebuild the test APK."
            )
        }

        private fun buildBasicAuth(username: String?, password: String?, fallback: String): BasicAuth {
            return if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                BasicAuth(username, password)
            } else {
                BasicAuth(fallback)
            }
        }

        fun getDefault(): BackendClient? = loadBackend(DEFAULT_BACKEND_NAME)

        fun loadBackend(connectionName: String): BackendClient {
            val secrets = loadSecrets(connectionName)
            validateRequiredSecrets(connectionName, secrets)

            return BackendClient(
                name = connectionName,
                backendUrl = secrets.backendUrl,
                webappUrl = secrets.webappUrl,
                backendWebsocket = secrets.backendWebsocket,
                basicAuth = buildBasicAuth(
                    secrets.basicAuthUsername,
                    secrets.basicAuthPassword,
                    secrets.basicAuthGeneral
                ),
                inbucketAuth = buildBasicAuth(
                    secrets.inbucketUsername,
                    secrets.inbucketPassword,
                    secrets.basicAuthGeneral
                ),
                domain = secrets.domain,
                deeplink = secrets.deeplink,
                inbucketUrl = secrets.inbucketUrl,
                inbucketUsername = secrets.inbucketUsername.orEmpty(),
                inbucketPassword = secrets.inbucketPassword.orEmpty(),
                keycloakUrl = secrets.keycloakUrl,
                acmeDiscoveryUrl = secrets.acmeDiscoveryUrl,
                k8sNamespace = secrets.k8sNamespace,
                socksProxy = secrets.socksProxy
            )
        }
    }

    init {

        if (socksProxy.isNotEmpty()) {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(
                        "qa",
                        BuildConfig.SOCKS_PROXY_PASSWORD_PASSWORD.toCharArray()
                    )
                }
            })
            if (proxy == null) {
                proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("socks.wire.link", 1080))
            }
        }
        // Register both backend and Inbucket origins so shared requests can use SOCKS when needed.
        NetworkBackendClient.registerProxy(backendUrl, proxy)
        NetworkBackendClient.registerProxy(inbucketUrl, proxy)
    }

    fun String.composeCompleteUrl(): String {
        return "${backendUrl}$this"
    }
}
