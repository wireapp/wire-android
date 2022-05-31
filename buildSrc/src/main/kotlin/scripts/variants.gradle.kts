package scripts

import CertificatePin
import ClientConfig
import ConfigFields
import ConfigType
import Configs
import Customization.defaultBuildtimeConfiguration
import FeatureFlags
import Features
import FlavourConfigs

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

        Configs.values().forEach { configs ->
            when (configs.configType) {
                ConfigType.STRING -> {
                    flavor.buildConfigField(
                        configs.configType.type, configs.name, """"${buildtimeConfiguration?.configuration?.get(configs.value)}""""
                    )
                }
                ConfigType.INT, ConfigType.BOOLEAN -> {
                    flavor.buildConfigField(
                        configs.configType.type, configs.name, buildtimeConfiguration?.configuration?.get(configs.value).toString()
                    )
                }
                ConfigType.CERTIFICATE_PIN -> {
                    val certificatePinMap = buildtimeConfiguration?.configuration?.get(Configs.CERTIFICATE_PIN.value) as Map<*, *>
                    CertificatePin.values().forEach { certificatePin ->
                        when (certificatePin.configType) {
                            ConfigType.STRING -> {
                                flavor.buildConfigField(
                                    certificatePin.configType.type,
                                    certificatePin.name,
                                    """"${certificatePinMap[certificatePin.value]}""""
                                )
                            }
                            ConfigType.INT, ConfigType.BOOLEAN -> {
                                flavor.buildConfigField(
                                    certificatePin.configType.type,
                                    certificatePin.name,
                                    certificatePinMap[certificatePin.value].toString()
                                )
                            }
                        }
                    }
                }

                ConfigType.FLAVOUR_CONFIG -> {
                    if (configs.value == flavor.name.toLowerCase()) {
                        val falvourMap = buildtimeConfiguration?.configuration?.get(flavor.name.toLowerCase()) as Map<*, *>

                        FlavourConfigs.values().forEach { flavourConfigs ->
                            when (flavourConfigs.configType) {
                                ConfigType.STRING -> {
                                    flavor.buildConfigField(
                                        flavourConfigs.configType.type,
                                        flavourConfigs.name,
                                        """"${falvourMap[flavourConfigs.value]}""""
                                    )
                                }
                                ConfigType.INT, ConfigType.BOOLEAN -> {
                                    flavor.buildConfigField(
                                        flavourConfigs.configType.type,
                                        flavourConfigs.name,
                                        falvourMap[flavourConfigs.value].toString()
                                    )
                                }
                            }
                        }
                    }

                }
            }

        }
    }
}
