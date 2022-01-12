buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter() //TODO Remove
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter() //TODO Remove
    }
}

plugins {
    id(ScriptPlugins.infrastructure)
}
