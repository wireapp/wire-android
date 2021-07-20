buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter() //TODO Remove by February 1, 2022
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter() //TODO Remove by February 1, 2022
    }
}

plugins {
    id(ScriptPlugins.infrastructure)
}
