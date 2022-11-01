buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    }
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.40")
        classpath("com.google.gms:google-services:4.3.10")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/wireapp/core-crypto")
            credentials {
                username = property("github.package_registry.user") as? String ?: System.getenv("GITHUB_USER")
                password = property("github.package_registry.token") as? String ?: System.getenv("GITHUB_TOKEN")
            }
        }
        maven { url = java.net.URI("https://jitpack.io") }
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    }
}

plugins {
    id(ScriptPlugins.infrastructure)
}
