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

    nexusUrl?.takeIf { it.isNotBlank() }?.let { url ->
        fun RepositoryHandler.addNexus() = repositories.maven {
            setUrl(url)
            isAllowInsecureProtocol = true //TODO Remove when nexusUrl is not needed anymore
        }
        buildscript.repositories.addNexus()
        allprojects { repositories.addNexus() }
    }
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
