buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    }
}

plugins {
    id(ScriptPlugins.infrastructure)
}
