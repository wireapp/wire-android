enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("klibs") {
            from(files("../kalium/gradle/libs.versions.toml"))
        }
    }
}
