/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package scripts

import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.ProductFlavor
import customization.ConfigType
import customization.Customization.getBuildtimeConfiguration
import customization.FeatureConfigs
import customization.FeatureFlags
import customization.Features

plugins { id("com.android.application") apply false }
// DO NOT USE CAPITAL LETTER FOR THE BUILD TYPE NAME OR JENKINS WILL BE MAD
object BuildTypes {
    const val DEBUG = "debug"
    const val RELEASE = "release"
    const val COMPAT = "compat"
    const val COMPAT_RELEASE = "compatrelease"
}

sealed class ProductFlavors(
    val buildName: String,
    val appName: String,
    val dimensions: String = FlavorDimensions.DEFAULT,
    val shareduserId: String = ""
) {
    override fun toString(): String = this.buildName

    object Dev : ProductFlavors("dev", "Wire Dev")
    object Staging : ProductFlavors("staging", "Wire Staging")

    object Beta : ProductFlavors("beta", "Wire Beta")
    object Internal : ProductFlavors("internal", "Wire Internal")
    object Production : ProductFlavors("prod", "Wire", shareduserId = "com.waz.userid")
}

object FlavorDimensions {
    const val DEFAULT = "default"
}

object Default {
    val BUILD_FLAVOR: String = System.getenv("flavor") ?: System.getenv("FLAVOR") ?: ProductFlavors.Dev.buildName
    val BUILD_TYPE = System.getenv("buildType") ?: System.getenv("BUILD_TYPE") ?: BuildTypes.DEBUG

    val BUILD_VARIANT = "${BUILD_FLAVOR.capitalize()}${BUILD_TYPE.capitalize()}"
}

fun NamedDomainObjectContainer<ApplicationProductFlavor>.createAppFlavour(
    applicationId: String,
    flavour: ProductFlavors
) {
    create(flavour.buildName) {
        dimension = flavour.dimensions
        applicationId = flavour.applicationId
        versionNameSuffix = "-${flavour.buildName}"
        resValue("string", "app_name", flavour.appName)
        manifestPlaceholders.apply {
            put("sharedUserId", flavour.shareduserId)
        }
    }
}


android {
    val enableSigning = System.getenv("ENABLE_SIGNING").equals("TRUE", true)
    if (enableSigning) {
        signingConfigs {
            maybeCreate(BuildTypes.RELEASE).apply {
                storeFile = file(System.getenv("KEYSTORE_FILE_PATH_RELEASE"))
                storePassword = System.getenv("KEYSTOREPWD_RELEASE")
                keyAlias = System.getenv("KEYSTORE_KEY_NAME_RELEASE")
                keyPassword = System.getenv("KEYPWD_RELEASE")
            }
            maybeCreate(BuildTypes.DEBUG).apply {
                storeFile = file(System.getenv("KEYSTORE_FILE_PATH_DEBUG"))
                storePassword = System.getenv("KEYSTOREPWD_DEBUG")
                keyAlias = System.getenv("KEYSTORE_KEY_NAME_DEBUG")
                keyPassword = System.getenv("KEYPWD_DEBUG")
            }
            maybeCreate(BuildTypes.COMPAT).apply {
                storeFile = file(System.getenv("KEYSTORE_FILE_PATH_COMPAT"))
                storePassword = System.getenv("KEYSTOREPWD_COMPAT")
                keyAlias = System.getenv("KEYSTORE_KEY_NAME_COMPAT")
                keyPassword = System.getenv("KEYPWD_COMPAT")
            }
            maybeCreate(BuildTypes.COMPAT_RELEASE).apply {
                storeFile = file(System.getenv("KEYSTORE_FILE_PATH_COMPAT_RELEASE"))
                storePassword = System.getenv("KEYSTOREPWD_COMPAT_RELEASE")
                keyAlias = System.getenv("KEYSTORE_KEY_NAME_COMPAT_RELEASE")
                keyPassword = System.getenv("KEYPWD_COMPAT_RELEASE")
            }
        }
    }

    buildTypes {
        getByName(BuildTypes.DEBUG) {
            isMinifyEnabled = false
            applicationIdSuffix = ".${BuildTypes.DEBUG}"
            isDebuggable = true
// Just in case a developer is trying to debug some prod crashes by turning on minify
            if (isMinifyEnabled) proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (enableSigning)
                signingConfig = signingConfigs.getByName("debug")
        }
        getByName(BuildTypes.RELEASE) {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
            if (enableSigning)
                signingConfig = signingConfigs.getByName("release")
        }
        create(BuildTypes.COMPAT) {
            initWith(getByName(BuildTypes.RELEASE))
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
            matchingFallbacks.add("release")
            if (enableSigning)
                signingConfig = signingConfigs.getByName("compat")
        }
        create(BuildTypes.COMPAT_RELEASE) {
            initWith(getByName(BuildTypes.RELEASE))
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
            matchingFallbacks.add("release")
            if (enableSigning)
                signingConfig = signingConfigs.getByName("compatrelease")
        }
    }

    val buildtimeConfiguration = getBuildtimeConfiguration(rootDir = rootDir)

    flavorDimensions(FlavorDimensions.DEFAULT)
    productFlavors {
        fun createFlavor(flavor: ProductFlavors) {
            val applicationId = buildtimeConfiguration.flavorMap[flavor.buildName]!![FeatureConfigs.APPLICATION_ID]!!
            createAppFlavour(applicationId, flavor)
        }
        createFlavor(ProductFlavors.Dev)
        createFlavour(ProductFlavors.Staging)
        createFlavour(ProductFlavors.Beta)
        createFlavour(ProductFlavors.Internal)
        createFlavour(ProductFlavors.Production)
    }


    /**
     * Process feature flags and if the feature is not included in a product flavor,
     * a default value of "false" or "deactivated" is used.
     *
     * @see "FeatureFlags.kt" file definition.
     */
    productFlavors.forEach { flavor ->
        Features.values().forEach { feature ->
            val activated = FeatureFlags.activated[flavor.name].orEmpty().contains(feature)
            flavor.buildConfigField("Boolean", feature.name, activated.toString())
        }

        FeatureConfigs.values().forEach { configs ->
            when (configs.configType) {
                ConfigType.STRING -> {
                    buildStringConfig(
                        flavor,
                        configs.configType.type,
                        configs.name,
                        buildtimeConfiguration.flavorMap[flavor.name]?.get(configs.value)?.toString()
                    )
                }

                ConfigType.INT, ConfigType.BOOLEAN -> {
                    buildNonStringConfig(
                        flavor,
                        configs.configType.type,
                        configs.name,
                        buildtimeConfiguration.flavorMap[flavor.name]?.get(configs.value).toString()
                    )
                }
            }
        }
    }
}


fun buildStringConfig(productFlavour: ProductFlavor, type: String, name: String, value: String?) {
    productFlavour.buildConfigField(
        type,
        name,
        value?.let { """"$it"""" } ?: "null"
    )
}

fun buildNonStringConfig(productFlavour: ProductFlavor, type: String, name: String, value: String) {
    productFlavour.buildConfigField(
        type,
        name,
        value
    )
}
