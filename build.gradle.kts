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
        maven { url = java.net.URI("https://jitpack.io") }
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        maven(url = "https://raw.githubusercontent.com/wireapp/wire-maven/main/releases")
    }
}

plugins {
    id(ScriptPlugins.infrastructure)
}
