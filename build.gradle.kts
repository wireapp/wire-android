buildscript {
    repositories {
        google()
        jcenter() //TODO Replace with mavenCentral
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

plugins {
    id(ScriptPlugins.infrastructure)
}
