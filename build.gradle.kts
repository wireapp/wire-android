buildscript {
    val nexusUrl = run {
        val urlEnvVar = "NEXUS_URL"
        val urlLocalVar = "nexus.url"
        val localPropertiesFileName = "local.properties"

        val properties = java.util.Properties()
        val propertiesFile = project.rootProject.file(localPropertiesFileName)

        return@run System.getenv(urlEnvVar) ?: run {
            properties.load(propertiesFile.inputStream())
            properties.getProperty(urlLocalVar)
        }
    }

    nexusUrl?.takeIf { it.isNotBlank() }?.let {
        buildscript.repositories.maven(it)
        allprojects { repositories.maven(it) }
    }
    repositories {
        google()
        jcenter()
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
