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
package com.wire.android.tests.core.config

import java.io.IOException
import java.net.URI
import java.util.Optional
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

class Config private constructor(private val configName: String) {
    companion object {
        private var currentInstance: Config? = null
        private var commonInstance: Config? = null
        private val cache = ConcurrentHashMap<String, Properties>()

        private const val CURRENT_CONFIG = "Configuration.properties"
        private const val COMMON_CONFIG = "CommonConfiguration.properties"

        @Synchronized
        fun current(): Config {
            if (currentInstance == null) {
                currentInstance = Config(CURRENT_CONFIG)
            }
            return currentInstance!!
        }

        @Synchronized
        fun common(): Config {
            if (commonInstance == null) {
                commonInstance = Config(COMMON_CONFIG)
            }
            return commonInstance!!
        }
    }

    private fun getValue(c: Class<*>, key: String): String {
        return getOptionalValue(c, key)
            .orElseThrow {
                IllegalArgumentException("There is no '$key' key in '$configName' config")
            }
    }

    private fun getOptionalValue(c: Class<*>, key: String): Optional<String> {
        return getOptionalValue(c, key, configName)
    }

    private fun getOptionalValue(c: Class<*>, key: String, resourcePath: String): Optional<String> {
        val cachedProps = cache.computeIfAbsent(resourcePath) { k ->
            try {
                c.classLoader.getResourceAsStream(resourcePath)?.use { stream ->
                    Properties().apply { load(stream) }
                } ?: throw IllegalArgumentException("Configuration file '$resourcePath' cannot be loaded")
            } catch (e: IOException) {
                throw IllegalArgumentException("Configuration file '$resourcePath' cannot be loaded", e)
            }
        }
        return Optional.ofNullable(cachedProps.getProperty(key))
    }

    fun getBackendType(c: Class<*>): String = getValue(c, "backendType")
    fun getBackendConnections(c: Class<*>): String = getValue(c, "backendConnections")
    fun getCustomBackendUrl(c: Class<*>): String = getValue(c, "customBackendUrl")
    fun getCustomBackendDomain(c: Class<*>): String = getValue(c, "customBackendDomain")
    fun getCustomBackendWebsocket(c: Class<*>): String = getValue(c, "customBackendWebsocket")
    fun getCustomBackendBasicAuth(c: Class<*>): String = getValue(c, "customBackendBasicAuth")

    fun isFeatureCheckConsentEnabled(c: Class<*>, backendType: String): Boolean {
        return when (backendType) {
            "qa-column-1", "qa-column-3", "bund-qa-column-1", "bund-qa-column-3",
            "bund-next-column-1", "bund-next-column-3", "kube", "calling-dev",
            "jct-66-a", "mobtown-test", "mobtown-red", "mobtown-ernie" -> false

            else -> true
        }
    }

    fun isFeaturePaymentsEnabled(c: Class<*>): Boolean {
        return when (getTeamAdminDomain(c).lowercase()) {
            "teams.wire.com", "wire-teams-staging.zinfra.io", "wire-teams-dev.zinfra.io" -> true
            else -> throw IllegalArgumentException("Not recognized team admin domain '${getTeamAdminDomain(c)}'")
        }
    }

    fun isFeatureServicesEnabled(c: Class<*>): Boolean {
        return when (getTeamAdminDomain(c).lowercase()) {
            "teams.wire.com", "wire-teams-staging.zinfra.io", "wire-teams-dev.zinfra.io" -> true
            else -> throw IllegalArgumentException("Not recognized team admin domain '${getTeamAdminDomain(c)}'")
        }
    }

    fun isFeatureLegalHoldEnabled(c: Class<*>): Boolean {
        return when (getTeamAdminDomain(c).lowercase()) {
            "wire-teams-dev.zinfra.io", "wire-teams-staging.zinfra.io" -> true
            "teams.wire.com" -> false
            else -> throw IllegalArgumentException("Not recognized team admin domain '${getTeamAdminDomain(c)}'")
        }
    }

    fun isFeatureSSOConfigurationEnabled(c: Class<*>): Boolean {
        return when (getTeamAdminDomain(c).lowercase()) {
            "wire-teams-dev.zinfra.io", "teams.wire.com", "wire-teams-staging.zinfra.io" -> true
            else -> throw IllegalArgumentException("Not recognized team admin domain '${getTeamAdminDomain(c)}'")
        }
    }

    fun isDigitalSignatureEnabled(c: Class<*>): Boolean {
        return when (getTeamAdminDomain(c).lowercase()) {
            "wire-teams-dev.zinfra.io", "teams.wire.com", "wire-teams-staging.zinfra.io" -> false
            else -> throw IllegalArgumentException("Not recognized team admin domain '${getTeamAdminDomain(c)}'")
        }
    }

    fun isDelegatedAdminEnabled(c: Class<*>): Boolean {
        return when (getTeamAdminDomain(c).lowercase()) {
            "wire-teams-dev.zinfra.io", "teams.wire.com" -> false
            "wire-teams-staging.zinfra.io" -> true
            else -> throw IllegalArgumentException("Not recognized team admin domain '${getTeamAdminDomain(c)}'")
        }
    }

    fun isOpenCVEnabled(c: Class<*>): Boolean = !getValue(c, "disableOpenCV").toBoolean()
    fun getDeviceName(c: Class<*>): String = getValue(c, "deviceName")
    fun getImagesPath(c: Class<*>): String = getValue(c, "defaultImagesPath")
    fun getAudioPath(c: Class<*>): String = getValue(c, "defaultAudioPath")
    fun getVideoPath(c: Class<*>): String = getValue(c, "defaultVideoPath")
    fun getMiscResourcesPath(c: Class<*>): String = getValue(c, "defaultMiscResourcesPath")
    fun getDefaultEmail(c: Class<*>): String = getValue(c, "defaultEmail")
    fun getDefaultEmailPassword(c: Class<*>): String = getValue(c, "defaultEmailPassword")
    fun getSpecialEmail(c: Class<*>): String = getValue(c, "specialEmail")
    fun getSpecialEmailPassword(c: Class<*>): String = getValue(c, "specialPassword")
    fun getDefaultEmailServer(c: Class<*>): String = getValue(c, "defaultEmailServer")
    fun getDriverTimeout(c: Class<*>): String = getValue(c, "driverTimeoutSeconds")
    fun getAppiumUrl(c: Class<*>): String = getValue(c, "appiumUrl")
    fun enableAppiumOutput(c: Class<*>): Boolean = getValue(c, "enableAppiumOutput") == "true"
    fun isSimulator(c: Class<*>): Boolean = getValue(c, "isSimulator") == "true"
    fun getIOSToolsRoot(c: Class<*>): String = getValue(c, "iOSToolsRoot")

    fun getWebAppApplicationPath(c: Class<*>): String {
        val path = getValue(c, "webappApplicationPath")
        require(path.isNotEmpty()) { "Could not find property: webappApplicationPath" }
        return path
    }

    fun getOverrrideWebappDockerImage(c: Class<*>): String = getValue(c, "overrrideWebappDockerImage")

    fun getWebsitePath(c: Class<*>): String {
        val path = getValue(c, "websitePath")
        require(path.isNotEmpty()) { "Could not find property: websitePath" }
        return path
    }

    fun getAccountPages(c: Class<*>): String = getValue(c, "accountPagesPath")
    fun getTeamAdminPath(c: Class<*>): String = getValue(c, "teamAdminPath")

    fun getTeamAdminDomain(c: Class<*>): String {
        return try {
            URI(getTeamAdminPath(c)).host ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun getBrowserDownloadPath(c: Class<*>): String = getValue(c, "browserDownloadPath")
    fun getAndroidApplicationPath(c: Class<*>): String = getValue(c, "androidApplicationPath")
    fun getIosApplicationPath(c: Class<*>): String = getValue(c, "iosApplicationPath")
    fun getAndroidMainActivity(c: Class<*>): String = getValue(c, "mainActivity")
    fun getAndroidShowLogcat(c: Class<*>): Boolean = getOptionalValue(c, "showLogcat").orElse("true").toBoolean()
    fun getAndroidLaunchActivity(c: Class<*>): String = getValue(c, "launchActivity")
    fun getOldAppPath(c: Class<*>): String = getValue(c, "oldAppPath")
    fun isCountlyAvailable(c: Class<*>): String = getValue(c, "isCountlyAvailable")
    fun getCurrentApkVersion(c: Class<*>): String = getValue(c, "currentApkVersion")
    fun getKubeConfigPath(c: Class<*>): String = getValue(c, "kubeConfigPath")
    fun getRealBuildNumber(c: Class<*>): Optional<String> = getOptionalValue(c, "realBuildNumber", configName)
    fun getBundleId(c: Class<*>): String = getValue(c, "bundleId")
    fun getAndroidPackage(c: Class<*>): String = getValue(c, "package")
    fun getMailboxHandlerType(c: Class<*>): String = getValue(c, "mailboxHandlerType")
    fun isTablet(c: Class<*>): Boolean = getOptionalValue(c, "isTablet").map { it.toBoolean() }.orElse(false)

    fun getCallingServiceUrl(c: Class<*>): String {
        val callingServiceUrl = getValue(c, "callingServiceUrl")
        return when {
            callingServiceUrl == "loadbalanced" -> "https://qa-callingservice-wire.runs.onstackit.cloud"
            callingServiceUrl.startsWith("http") -> callingServiceUrl
            else -> throw RuntimeException("Missing or wrong callingServiceUrl: $callingServiceUrl")
        }
    }

    fun getCallingServiceEnvironment(c: Class<*>): String = getValue(c, "com.wire.calling.env").uppercase()

    fun getDefaultEmailListenerUrl(c: Class<*>): String {
        return if (isRunningOnJenkinsNode() && !isDesktop()) {
            getValue(c, "defaultInternalEmailListenerUrl")
        } else {
            getValue(c, "defaultEmailListenerUrl")
        }
    }

    fun isScreenshootingEnabled(c: Class<*>): Boolean = getValue(c, "makeScreenshots").toBoolean()

    fun getTestrailServerUrl(c: Class<*>): String {
        return if (isRunningOnJenkinsNode()) {
            getValue(c, "internalTestrailServerUrl")
        } else {
            getValue(c, "testrailServerUrl")
        }
    }

    private fun isDesktop(): Boolean {
        return System.getProperty("com.wire.app.path") != null
    }

    private fun isRunningOnJenkinsNode(): Boolean {
        return System.getenv("BUILD_NUMBER") != null
    }

    fun getTestrailUsername(c: Class<*>): String = getValue(c, "testrailUser")
    fun getTestrailToken(c: Class<*>): String = getValue(c, "testrailToken")
    fun getTestrailProjectName(c: Class<*>): Optional<String> = getOptionalValue(c, "testrailProjectName")
    fun getTestrailPlanName(c: Class<*>): Optional<String> = getOptionalValue(c, "testrailPlanName")
    fun getTestrailRunName(c: Class<*>): Optional<String> = getOptionalValue(c, "testrailRunName")
    fun getTestrailRunConfigName(c: Class<*>): Optional<String> = getOptionalValue(c, "testrailRunConfigName")
    fun getTestinyProjectName(c: Class<*>): Optional<String> = getOptionalValue(c, "testinyProjectName")
    fun getTestinyRunName(c: Class<*>): Optional<String> = getOptionalValue(c, "testinyRunName")
    fun getJiraUrl(c: Class<*>): Optional<String> = getOptionalValue(c, "jiraUrl")
    fun getSyncIsAutomated(c: Class<*>): Boolean = getValue(c, "syncIsAutomated").lowercase() == "true"
    fun getBuildPath(c: Class<*>): String = getValue(c, "projectBuildPath")
    fun getPlatformVersion(c: Class<*>): String = getValue(c, "platformVersion")
    fun getIOSAppName(c: Class<*>): String = getValue(c, "appName")
    fun getTestServiceUrl(c: Class<*>): String = getValue(c, "testServiceUrl")
    fun getOldTestServiceUrl(c: Class<*>): String = getValue(c, "oldTestserviceUrl")
    fun getRcTestsCommentPath(c: Class<*>): Optional<String> = getOptionalValue(c, "rcTestsCommentPath")
    fun getCucumberReportUrl(c: Class<*>): Optional<String> = getOptionalValue(c, "cucumberReportUrl")
    fun getAndroidToolsPath(c: Class<*>): String = getValue(c, "androidToolsPath")
    fun getPackageAutoDetection(c: Class<*>): String = getValue(c, "packageAutoDetection")
    fun isOnGrid(c: Class<*>): Boolean = getValue(c, "isOnGrid").toBoolean()
    fun getBrowserName(c: Class<*>): String = getValue(c, "browserName")
    fun getEnforceAppInstall(c: Class<*>): Boolean = getValue(c, "enforceAppInstall").toBoolean()
    fun getUDID(c: Class<*>): Optional<String> = getOptionalValue(c, "UDID")
    fun getTestingGalleryPath(c: Class<*>): String = getValue(c, "testingGalleryPath")
}
