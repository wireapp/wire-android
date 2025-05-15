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
package com.wire.android.tests.core.services.backend


import com.wire.android.tests.core.config.BackendConnectionsReader
import com.wire.android.tests.core.config.Config
import com.wire.android.tests.core.models.BasicAuth
import com.wire.android.tests.core.models.ClientUser
import com.wire.android.tests.core.utils.ZetaLogger
import org.json.JSONArray
import java.util.logging.Logger

class BackendConnections {
//    companion object {
        private val log: Logger = ZetaLogger.getLog(BackendConnections::class.simpleName)
        private val backendConnections: Map<String, Backend>
        private val defaultBackendName: String

        init {
            backendConnections = getBackendConnections()
            defaultBackendName = Config.common().getBackendType(BackendConnections::class.java)
        }

        private fun getBackendConnections(): Map<String, Backend> {
            val backendConnections = mutableMapOf<String, Backend>()
            val reader = BackendConnectionsReader()
            val json = reader.read()
            for (i in 0 until json.length()) {
                val entry = json.getJSONObject(i)
                if (entry.has("fields")) {
                    val fields = mapFields(entry.getJSONArray("fields"))
                    val name = fields["name"]
                    log.info("1Password item for $name with ${fields.size} entries found.")
                    val basicAuth = if (fields["basicAuthUsername"] != null && fields["basicAuthPassword"] != null) {
                        val username = fields["basicAuthUsername"] ?: throw IllegalArgumentException("Username is required")
                        val password = fields["basicAuthPassword"] ?: throw IllegalArgumentException("Password is required")
                        BasicAuth(username, password)
                    } else {
                        val basicAuth = fields["basicAuth"] ?: throw IllegalArgumentException("basicAuth is required")
                        BasicAuth(basicAuth)
                    }
                    val inbucketAuth = if (fields["inbucketUsername"] != null && fields["inbucketPassword"] != null) {
                        val inbucketUsername = fields["inbucketUsername"] ?: throw IllegalArgumentException("inbucketUsername is required")
                        val inbucketPassword = fields["inbucketPassword"] ?: throw IllegalArgumentException("inbucketPassword is required")
                        BasicAuth(inbucketUsername, inbucketPassword)
                    } else {
                        basicAuth
                    }
                    if (name != null) {
                        backendConnections[name] = Backend(
                            name,
                            fields["backendUrl"],
                            fields["webappUrl"],
                            fields["domain"],
                            fields["backendWebsocket"],
                            fields["deeplink"],
                            fields["inbucketUrl"],
                            fields["keycloakUrl"],
                            fields["acmeDiscoveryUrl"],
                            fields["k8sNamespace"],
                            basicAuth,
                            inbucketAuth,
                            false,
                            fields["socksProxy"]
                        )
                    }
                }
            }
            return backendConnections
        }

        private fun mapFields(fields: JSONArray): Map<String, String> {
            val map = mutableMapOf<String, String>()
            for (i in 0 until fields.length()) {
                val field = fields.getJSONObject(i)
                if (field.has("label") && field.has("value")) {
                    val label = field.getString("label")
                    val value = field.getString("value")
                    map[label] = value
                }
            }
            return map
        }

        fun getDefault(): Backend {
            return backendConnections[defaultBackendName]!!
        }

        fun get(backendName: String?): Backend {
            var name = backendName ?: getDefault().getBackendName()
            // Possibility to use bund-qa and bund-next environments by just using the prefix "column-x" as backend name
            if (name != null) {
                if ((name.startsWith("column") || name.startsWith("external")) && defaultBackendName.contains("column")) {
                    val prefix = defaultBackendName.substring(0, defaultBackendName.indexOf("column"))
                    log.info(String.format("Replace %s with %s", name, prefix + name))
                    name = prefix + name
                }
            }
            return backendConnections.getOrDefault(name, getDefault())
        }

        fun get(user: ClientUser): Backend {
            return backendConnections.getOrDefault(user.backendName, getDefault())
        }
//    }
}
