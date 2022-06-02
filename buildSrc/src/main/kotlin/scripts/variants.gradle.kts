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
import com.android.build.api.dsl.ProductFlavor

plugins { id("com.android.application") apply false }

object BuildTypes {
    const val DEBUG = "debug"
    const val RELEASE = "release"
}

object ProductFlavors {
    const val DEV = "dev"
    const val INTERNAL = "internal"
    const val PUBLIC = "public"
}

private object FlavorDimensions {
    const val DEFAULT = "default"
}

object Default {
    val BUILD_FLAVOR = System.getenv("flavor") ?: ProductFlavors.DEV
    val BUILD_TYPE = System.getenv("buildType") ?: BuildTypes.DEBUG

    val BUILD_VARIANT = "${BUILD_FLAVOR.capitalize()}${BUILD_TYPE.capitalize()}"
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
    }

    flavorDimensions(FlavorDimensions.DEFAULT)
    productFlavors {
        create(ProductFlavors.DEV) {
            dimension = FlavorDimensions.DEFAULT
            applicationIdSuffix = ".${ProductFlavors.DEV}"
            versionNameSuffix = "-${ProductFlavors.DEV}"
        }
        create(ProductFlavors.INTERNAL) {
            dimension = FlavorDimensions.DEFAULT
            applicationIdSuffix = ".${ProductFlavors.INTERNAL}"
            versionNameSuffix = "-${ProductFlavors.INTERNAL}"
        }
        create(ProductFlavors.PUBLIC) {
            dimension = FlavorDimensions.DEFAULT
        }
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

fun buildFlavorConfig(productFlavour: ProductFlavor, configs: FeatureConfigs, buildTimeConfiguration: Customization.BuildTimeConfiguration?) {
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

