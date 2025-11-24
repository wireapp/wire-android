/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package scripts

import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.ProductFlavor
import customization.ConfigType
import customization.Customization.getBuildtimeConfiguration
import customization.FeatureConfigs
import customization.FeatureFlags
import customization.Features
import customization.overrideResourcesForAllFlavors
import flavor.FlavorDimensions
import flavor.ProductFlavors

plugins { id("com.android.application") apply false }

fun NamedDomainObjectContainer<ApplicationProductFlavor>.createAppFlavour(
    flavorApplicationId: String,
    sharedUserId: String,
    flavour: ProductFlavors
) {
    create(flavour.buildName) {
        dimension = flavour.dimensions
        applicationId = flavorApplicationId
        versionNameSuffix = flavour.versionNameSuffix
        resValue("string", "app_name", flavour.appName)
        manifestPlaceholders["sharedUserId"] = sharedUserId
        manifestPlaceholders["appAuthRedirectScheme"] = flavorApplicationId
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
            maybeCreate(BuildTypes.BENCHMARK).apply {
                storeFile = file(System.getenv("KEYSTORE_FILE_PATH_DEBUG"))
                storePassword = System.getenv("KEYSTOREPWD_DEBUG")
                keyAlias = System.getenv("KEYSTORE_KEY_NAME_DEBUG")
                keyPassword = System.getenv("KEYPWD_DEBUG")
            }
        }
    }

    buildTypes {
        
        getByName(BuildTypes.DEBUG) {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".${BuildTypes.DEBUG}"
            isDebuggable = true
            // Just in case a developer is trying to debug some prod crashes by turning on minify
            if (isMinifyEnabled) proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (enableSigning)
                signingConfig = signingConfigs.getByName("debug")
        }
        getByName(BuildTypes.RELEASE) {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
            signingConfig = if (enableSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        create(BuildTypes.COMPAT) {
            initWith(getByName(BuildTypes.RELEASE))
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
            matchingFallbacks.add("release")
            if (enableSigning)
                signingConfig = signingConfigs.getByName("compat")
        }
        create(BuildTypes.COMPAT_RELEASE) {
            initWith(getByName(BuildTypes.RELEASE))
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
            matchingFallbacks.add("release")
            if (enableSigning)
                signingConfig = signingConfigs.getByName("compatrelease")
        }
        create(BuildTypes.BENCHMARK) {
            initWith(getByName(BuildTypes.RELEASE))
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
            matchingFallbacks.add("release")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    flavorDimensions.add(FlavorDimensions.DEFAULT)

    val buildtimeConfiguration = getBuildtimeConfiguration(rootDir = rootDir)
    val flavorMap = buildtimeConfiguration.flavorSettings.flavorMap

    productFlavors {
        fun createFlavor(flavor: ProductFlavors) {
            val flavorName = flavor.buildName
            val flavorSpecificMap = flavorMap[flavorName]
            requireNotNull(flavorSpecificMap) {
                "Missing configs in json file for the flavor '$flavorName'"
            }
            val flavorApplicationId = flavorSpecificMap[FeatureConfigs.APPLICATION_ID.value] as? String
            requireNotNull(flavorApplicationId) {
                "Missing application ID definition for the flavor '$flavorName'"
            }
            // prefer value from FeatureConfigs if defined, otherwise fallback to in-code flavor value.
            val userId: String = (flavorSpecificMap[FeatureConfigs.USER_ID.value] as? String) ?: flavor.shareduserId
            createAppFlavour(
                flavorApplicationId = flavorApplicationId,
                sharedUserId = userId,
                flavour = flavor
            )
        }
        ProductFlavors.all.forEach(::createFlavor)
    }

    buildtimeConfiguration.customResourceOverrideDirectory?.let {
        overrideResourcesForAllFlavors(it)
    }

    /**
     * Process feature flags and if the feature is not included in a product flavor,
     * a default value of "false" or "deactivated" is used.
     *
     * @see "FeatureFlags.kt" file definition.
     */
    productFlavors.forEach { flavor ->
        Features.values().forEach { feature ->
            val activated = FeatureFlags.activated.mapKeys { it.key.buildName }[flavor.name].orEmpty().contains(feature)
            flavor.buildConfigField("Boolean", feature.name, activated.toString())
        }

        FeatureConfigs.values().forEach { configs ->
            when (configs.configType) {
                ConfigType.STRING -> {
                    if (FeatureConfigs.GOOGLE_API_KEY.value == configs.value) {
                        val apiKey:String? = flavorMap[flavor.name]?.get(configs.value)?.toString()
                        flavor.manifestPlaceholders["GCP_API_KEY"] = apiKey ?: ""
                    } else {
                        buildStringConfig(
                            flavor,
                            configs.configType.type,
                            configs.name,
                            flavorMap[flavor.name]?.get(configs.value)?.toString()
                        )
                    }
                }

                ConfigType.INT,
                ConfigType.BOOLEAN -> {
                    buildNonStringConfig(
                        flavor,
                        configs.configType.type,
                        configs.name,
                        flavorMap[flavor.name]?.get(configs.value).toString()
                    )
                }

                ConfigType.MapOfStringToListOfStrings -> {
                    val map = flavorMap[flavor.name]?.get(configs.value) as? Map<*, *>
                    val mapString = map?.map { (key, value) ->
                        "\"$key\", java.util.Arrays.asList(${(value as? List<*>)?.joinToString { "\"$it\"" } ?: ""})".let {
                            "put($it);"
                        }
                    }?.joinToString(",\n") ?: ""
                    buildNonStringConfig(
                        flavor,
                        configs.configType.type,
                        configs.name,
                        "new java.util.HashMap<String, java.util.List<String>>() {{\n$mapString\n}}"
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
