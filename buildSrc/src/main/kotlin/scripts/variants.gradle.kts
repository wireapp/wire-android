package scripts

plugins { id("core.android") }

private enum class BuildTypes(val value: String) {
    DEBUG("debug"), RELEASE("release")
}

private enum class ProductFlavors(val value: String) {
    DEV("dev"), INTERNAL("internal"), PUBLIC("public")
}

private enum class FlavorDimensions(val value: String) {
    VERSION("version")
}

android {
    buildTypes {
        getByName(BuildTypes.DEBUG.value) {
            isMinifyEnabled = false
            applicationIdSuffix = ".${BuildTypes.DEBUG.value}"
            isDebuggable = true
        }
        getByName(BuildTypes.RELEASE.value) {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions(FlavorDimensions.VERSION.value)
    productFlavors {
        create(ProductFlavors.DEV.value) {
            dimension = FlavorDimensions.VERSION.value
            applicationIdSuffix = ".${ProductFlavors.DEV.value}"
            versionNameSuffix = "-${ProductFlavors.DEV.value}"
        }
        create(ProductFlavors.INTERNAL.value) {
            dimension = FlavorDimensions.VERSION.value
            applicationIdSuffix = ".${ProductFlavors.INTERNAL.value}"
            versionNameSuffix = "-${ProductFlavors.INTERNAL.value}"
        }
        create(ProductFlavors.PUBLIC.value) {
            dimension = FlavorDimensions.VERSION.value
        }
    }
}