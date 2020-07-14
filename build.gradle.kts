plugins {
    id(ScriptPlugins.infrastructure)
    id(ScriptPlugins.detekt)
    id(ScriptPlugins.jacoco)
}

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath(BuildPlugins.androidGradlePlugin)
        classpath(BuildPlugins.kotlinGradlePlugin)
        classpath(BuildPlugins.jacocoGradlePlugin)
        classpath(BuildPlugins.detektGradlePlugin)
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}