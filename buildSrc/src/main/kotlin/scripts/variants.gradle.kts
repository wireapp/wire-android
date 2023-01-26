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

import CertificatePin
import ClientConfig
import ConfigFields
import ConfigType
import Customization
import Customization.defaultBuildtimeConfiguration
import FeatureConfigs
import FeatureFlags
import Features
import FlavourConfigs
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.ProductFlavor

plugins { id("com.android.application") apply false }

object BuildTypes {
    const val DEBUG = "debug"
    const val RELEASE = "release"
    const val COMPAT = "compat"
}

sealed class ProductFlavors(
    val applicationId: String,
    val buildName: String,
    val appName: String,
    val applicationIdSuffix: String? = null,
    val dimensions: String = FlavorDimensions.DEFAULT
) {
    override fun toString(): String = this.buildName

    object Dev : ProductFlavors("com.waz.zclient.dev", "dev", "Wire Dev")
    object Staging : ProductFlavors("com.waz.zclient.dev", "staging", "Wire Staging")

    object Beta : ProductFlavors("com.wire.android", "beta", "Wire Beta", applicationIdSuffix = "internal")
    object Internal : ProductFlavors("com.wire", "internal","Wire Internal", applicationIdSuffix = "internal")
    object Production : ProductFlavors("com.wire", "prod", "Wire")
}

object FlavorDimensions {
    const val DEFAULT = "default"
}

object Default {
    val BUILD_FLAVOR: String = System.getenv("flavor") ?: ProductFlavors.Dev.buildName
    val BUILD_TYPE = System.getenv("buildType") ?: BuildTypes.DEBUG

    val BUILD_VARIANT = "${BUILD_FLAVOR.capitalize()}${BUILD_TYPE.capitalize()}"
}

 fun NamedDomainObjectContainer<ApplicationProductFlavor>.createAppFlavour(flavour : ProductFlavors) {
    create(flavour.buildName) {
        dimension = flavour.dimensions
        applicationId = flavour.applicationId
        versionNameSuffix = "-${flavour.buildName}"
        if(!flavour.applicationIdSuffix.isNullOrBlank()) {
            applicationIdSuffix = ".${flavour.applicationIdSuffix}"
        }
        resValue("string", "app_name", flavour.appName)
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
    }

    flavorDimensions(FlavorDimensions.DEFAULT)
    productFlavors {
        createAppFlavour(ProductFlavors.Dev)
        createAppFlavour(ProductFlavors.Staging)
        createAppFlavour(ProductFlavors.Beta)
        createAppFlavour(ProductFlavors.Internal)
        createAppFlavour(ProductFlavors.Production)
    }

    /**
     * Process client configuration properties.
     *
     * @see "ClientConfig.kt" file definition.
     */
    val buildtimeConfiguration = defaultBuildtimeConfiguration(rootDir = rootDir)

    buildTypes.map { type ->
        ConfigFields.values().forEach { configField ->
            val configValuesMap = ClientConfig.properties[type.name].orEmpty()
            if (configValuesMap.isNotEmpty()) {
                type.buildConfigField(
                    "String", configField.name,
                    configValuesMap[configField] ?: configField.defaultValue
                )
            }
        }
    }

    /**
     * Process feature flags and if the feature is not included in a product flavor,
     * a default value of "false" or "deactivated" is used.
     *
     * @see "FeatureFlags.kt" file definition.
     */
    productFlavors.map { flavor ->
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
                        buildtimeConfiguration?.configuration?.get(configs.value).toString()
                    )
                }

                ConfigType.INT, ConfigType.BOOLEAN -> {
                    buildNonStringConfig(
                        flavor,
                        configs.configType.type, configs.name, buildtimeConfiguration?.configuration?.get(configs.value).toString()
                    )
                }

                ConfigType.CERTIFICATE_PIN -> {
                    buildCertificatePinConfig(flavor, buildtimeConfiguration)
                }

                ConfigType.FLAVOUR_CONFIG -> {
                    buildFlavorConfig(flavor, configs, buildtimeConfiguration)

                }
            }

        }
    }

}

fun buildFlavorConfig(
    productFlavour: ProductFlavor,
    configs: FeatureConfigs,
    buildTimeConfiguration: Customization.BuildTimeConfiguration?
) {
    if (configs.value == productFlavour.name.toLowerCase()) {
        val falvourMap = buildTimeConfiguration?.configuration?.get(productFlavour.name.toLowerCase()) as Map<*, *>

        FlavourConfigs.values().forEach { flavourConfigs ->
            when (flavourConfigs.configType) {
                ConfigType.STRING -> {
                    buildStringConfig(
                        productFlavour,
                        flavourConfigs.configType.type,
                        flavourConfigs.name,
                        falvourMap[flavourConfigs.value].toString()
                    )
                }

                ConfigType.INT, ConfigType.BOOLEAN -> {
                    buildNonStringConfig(
                        productFlavour, flavourConfigs.configType.type,
                        flavourConfigs.name,
                        falvourMap[flavourConfigs.value].toString()
                    )
                }
            }
        }
    }
}


fun buildCertificatePinConfig(productFlavour: ProductFlavor, buildTimeConfiguration: Customization.BuildTimeConfiguration?) {
    val certificatePinMap = buildTimeConfiguration?.configuration?.get(FeatureConfigs.CERTIFICATE_PIN.value) as Map<*, *>
    CertificatePin.values().forEach { certificatePin ->
        when (certificatePin.configType) {
            ConfigType.STRING -> {
                buildStringConfig(
                    productFlavour, certificatePin.configType.type,
                    certificatePin.name,
                    certificatePinMap[certificatePin.value].toString()
                )
            }

            ConfigType.INT, ConfigType.BOOLEAN -> {
                buildNonStringConfig(
                    productFlavour,
                    certificatePin.configType.type,
                    certificatePin.name,
                    certificatePinMap[certificatePin.value].toString()
                )
            }
        }
    }
}

fun buildStringConfig(productFlavour: ProductFlavor, type: String, name: String, value: String) {
    productFlavour.buildConfigField(
        type,
        name,
        """"$value""""
    )

}

fun buildNonStringConfig(productFlavour: ProductFlavor, type: String, name: String, value: String) {
    productFlavour.buildConfigField(
        type,
        name,
        value
    )

}
