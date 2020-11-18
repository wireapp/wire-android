package scripts

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
    const val BUILD_TYPE = BuildTypes.DEBUG
    const val BUILD_FLAVOR = ProductFlavors.DEV

    val BUILD_VARIANT = "${BUILD_FLAVOR.capitalize()}${BUILD_TYPE.capitalize()}"
}

android {
    buildTypes {
        getByName(BuildTypes.DEBUG) {
            isMinifyEnabled = false
            applicationIdSuffix = ".${BuildTypes.DEBUG}"
            isDebuggable = true
        }
        getByName(BuildTypes.RELEASE) {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    buildTypes.map { type ->
        ConfigFields.values().forEach { configField ->
            val configValuesMap = ClientConfig.properties[type.name].orEmpty()
            if (configValuesMap.isNotEmpty()) {
                type.buildConfigField("String", configField.name,
                    configValuesMap[configField] ?: configField.defaultValue)
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
    }
}
