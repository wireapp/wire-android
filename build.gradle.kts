plugins {
    id(ScriptPlugins.infrastructure)
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