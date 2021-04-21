

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
        ?: throw RuntimeException("Missing Nexus URL at $propertiesFile, or environment variable $urlEnvVar")
    }

    if (nexusUrl.isNotBlank()) {
        buildscript {
            repositories {
                maven(nexusUrl)
            }
        }
        allprojects {
            repositories {
                maven(nexusUrl)
            }
        }
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
