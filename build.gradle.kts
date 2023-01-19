buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    }
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.40")
        classpath("com.google.gms:google-services:4.3.14")
    }
}

allprojects {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/wireapp/core-crypto")
            credentials {
                username = getLocalProperty("github.package_registry.user", System.getenv("GITHUB_USER"))
                password = getLocalProperty("github.package_registry.token", System.getenv("GITHUB_TOKEN"))
            }
        }
        maven { url = java.net.URI("https://jitpack.io") }
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")

        // TODO we should remove this and "localrepo" dir after cryptobox-android debugging is completed
        val avsLocal = maven(url = uri("$rootDir/kalium/localrepo/"))
        exclusiveContent {
            forRepositories(avsLocal)
            filter {
                includeModule("com.wire", "cryptobox-android")
            }
        }
    }
}

plugins {
    id(ScriptPlugins.infrastructure)
}
