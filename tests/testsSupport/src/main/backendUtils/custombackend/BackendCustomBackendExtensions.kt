@file:Suppress("PackageNaming")
/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package backendUtils.custombackend

import backendUtils.BackendClient
import backendUtils.auth.defaultheaders
import network.NetworkBackendClient
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun BackendClient.claimDomain(domain: String, configUrl: String, webappUrl: String) {
    registerCustomBackend(domain, configUrl, webappUrl)

    registerDomainClaim(
        domain = domain,
        body = JSONObject().apply {
            put(
                "backend",
                JSONObject().apply {
                    put("config_url", customBackendUrl(domain).toString())
                    put("webapp_url", webappUrl)
                }
            )
            put("domain_redirect", "backend")
            put("team_invite", "not-allowed")
        }
    )
}

fun BackendClient.claimSsoDomain(domain: String, ssoCode: String) {
    registerDomainClaim(
        domain = domain,
        body = JSONObject().apply {
            put("domain_redirect", "sso")
            // Domain registration expects the identity-provider UUID without the user-facing "wire-" prefix.
            put("sso_code", ssoCode.removePrefix("wire-"))
            put("team_invite", "not-allowed")
        }
    )
}

fun BackendClient.deleteDomainClaim(domain: String) {
    try {
        deleteDomainRegistration(domain)
    } finally {
        deleteCustomBackend(domain)
    }
}

fun BackendClient.deleteSsoDomainClaim(domain: String) {
    deleteDomainRegistration(domain)
}

private fun BackendClient.registerCustomBackend(domain: String, configUrl: String, webappUrl: String) {
    NetworkBackendClient.sendJsonRequest(
        url = customBackendUrl(domain, internal = true),
        method = "PUT",
        body = JSONObject().apply {
            put("config_json_url", configUrl)
            put("webapp_welcome_url", webappUrl)
        }.toString(),
        headers = internalApiHeaders()
    )
}

private fun BackendClient.deleteCustomBackend(domain: String) {
    NetworkBackendClient.sendJsonRequest(
        url = customBackendUrl(domain, internal = true),
        method = "DELETE",
        body = JSONObject().toString(),
        headers = internalApiHeaders()
    )
}

private fun BackendClient.registerDomainClaim(domain: String, body: JSONObject) {
    NetworkBackendClient.sendJsonRequest(
        url = domainRegistrationUrl(domain),
        method = "PUT",
        body = body.toString(),
        headers = internalApiHeaders()
    )
}

private fun BackendClient.deleteDomainRegistration(domain: String) {
    NetworkBackendClient.sendJsonRequest(
        url = domainRegistrationUrl(domain),
        method = "DELETE",
        body = JSONObject().toString(),
        headers = internalApiHeaders()
    )
}

private fun BackendClient.domainRegistrationUrl(domain: String) =
    URI(
        "domain-registration/${URLEncoder.encode(domain, StandardCharsets.UTF_8.toString())}"
            .composeInternalApiUrl()
    ).toURL()

private fun BackendClient.customBackendUrl(domain: String, internal: Boolean = false) =
    URI(
        "custom-backend/by-domain/${URLEncoder.encode(domain, StandardCharsets.UTF_8.toString())}"
            .let { if (internal) it.composeInternalApiUrl() else it.composeUnversionedUrl() }
    ).toURL()

private fun BackendClient.internalApiHeaders() = defaultheaders.toMutableMap().apply {
    put(BackendClient.AUTHORIZATION, basicAuth.getEncoded())
}
