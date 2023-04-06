/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */


import groovy.json.JsonSlurper
import java.io.File
import java.util.Properties


object Customization {

    internal const val KEY_FLAVORS = "flavors"
    internal const val CUSTOM_JSON_FILE_NAME = "custom.json"
    internal const val DEFAULT_JSON_FILE_NAME = "default.json"

    private val jsonReader = JsonSlurper()
    private val properties = java.util.Properties().apply {
        val localProperties = File("local.properties")
        if (localProperties.exists()) {
            load(localProperties.inputStream())
        }
    }

    fun getBuildtimeConfiguration(
        rootDir: File, customizationCoordinates: CustomizationCoordinates? = null
    ): NormalizedFlavorSettings {
        val defaultConfigFile = File("$rootDir/$DEFAULT_JSON_FILE_NAME")
        val defaultConfig = loadConfigsFromFile(defaultConfigFile)

        return when (customizationCoordinates) {
            is CustomizationCoordinates.NoCustomization -> defaultConfig
            is CustomizationCoordinates.Values -> getCustomizedConfiguration(defaultConfig, rootDir, customizationCoordinates)
            null -> {
                val isCustomBuild = properties.readCustomizationProperty(CustomizationProperty.CUSTOM_REPOSITORY) != null
                if (isCustomBuild) {
                    getCustomizedConfiguration(defaultConfig, rootDir, CustomizationCoordinates.fromProperties(properties))
                } else {
                    defaultConfig
                }
            }
        }
    }

    fun loadConfigsFromFile(file: File): NormalizedFlavorSettings {
        val importedMap = jsonReader.parseText(file.readText())
        require(importedMap is Map<*, *>) {
            "Imported file '$file' isn't recognised as a JSON object"
        }
        importedMap as? Map<String, *> ?: throw IllegalArgumentException(
            "Imported file '$file' could not be properly deserialized into a JSON object"
        )
        return normalizeFlavorOverrides(importedMap)
    }

    /**
     * Takes a Map with top-level configs and flavor-specific overrides,
     * For example:
     * ```json
     * {
     *     "color": "red",
     *     "flavors": {
     *         "apple": { },
     *         "strawberry": { },
     *         "cabbage": {
     *             "color": "green"
     *         }
     *     }
     * }
     * ```
     *
     * Outputs a flat customisation map where flavors are on the top level
     * and default values are duplicated inside them:
     *
     * ```json
     * {
     *     "apple": {
     *         "color": "red"
     *     },
     *     "strawberry": {
     *         "color": "red"
     *     },
     *     "cabbage": {
     *         "color": "green"
     *     }
     * }
     * ```
     */
    private fun normalizeFlavorOverrides(configs: Map<String, Any?>): NormalizedFlavorSettings {
        val flavorSets = configs[KEY_FLAVORS]
        requireNotNull(flavorSets) {
            "Can't normalize map as it does not contain a flavor list"
        }
        require(flavorSets is Map<*, *>) {
            "The 'flavors' map entry should contain another map of config overrides"
        }
        val topLevelConfigs = configs.filter { it.key != KEY_FLAVORS }

        val normalizedMap = flavorSets.map { (flavorName, overrides) ->
            require(flavorName is String) {
                "The flavor $flavorName is not named using a valid String"
            }
            require(overrides is Map<*, *>) {
                "The entry '$flavorName' is not a valid map containing config overrides"
            }
            val overwrittenTopLevelConfigs = topLevelConfigs.overwritingWith(flavorName, overrides)
            flavorName to overwrittenTopLevelConfigs
        }.toMap()

        return NormalizedFlavorSettings(normalizedMap)
    }

    /**
     * Creates a copy of [this], replacing all its values
     * with the values specified in [overrides].
     *
     * Similar to [Map.plus], but this provides a more granular
     * exception handling and customisation of the override condition, if desired.
     */
    private fun Map<String, Any?>.overwritingWith(
        flavorName: String, overrides: Map<*, *>
    ): Map<String, Any?> {
        val copy = this.toMutableMap()
        overrides.forEach { overrideName, overrideValue ->
            require(overrideName is String) {
                "The entry named '$overrideName' for the flavor '$flavorName' is not a valid string ?!"
            }
            copy[overrideName] = overrideValue
        }
        return copy
    }

    //  Will check out custom repo, if any, and load its configuration, merging it on top of the default configuration
    private fun getCustomizedConfiguration(
        defaultConfig: NormalizedFlavorSettings, rootDir: File, propertyCoordinates: CustomizationCoordinates.Values
    ): NormalizedFlavorSettings = with(propertyCoordinates) {
        val customCheckoutDir = File(rootDir, "custom")

        val customConfigFile = File(customCheckoutDir, "$customFolder/$clientFolder/$CUSTOM_JSON_FILE_NAME")

        if (customCheckoutDir.exists()) {
            customCheckoutDir.deleteRecursively()
        }

//            val credentials = Credentials(grGitUser, grGitPassword)
//            Grgit.clone(mapOf("dir" to customCheckoutDir, "uri" to customRepository, "credentials" to credentials))

        return getCustomBuildConfigs(customConfigFile, defaultConfig)
    }

    private fun getCustomBuildConfigs(customConfigFile: File, defaultConfig: NormalizedFlavorSettings): NormalizedFlavorSettings {
        val customConfig = loadConfigsFromFile(customConfigFile)

        customConfig.flavorMap.keys.forEach { customFlavor ->
            require(defaultConfig.flavorMap.containsKey(customFlavor)) {
                """
                |Flavor '$customFlavor' defined in $CUSTOM_JSON_FILE_NAME does not have a matching definition in $DEFAULT_JSON_FILE_NAME.
                |Check for a typo in the name of '$customFlavor' in $CUSTOM_JSON_FILE_NAME, and make sure the default 
                |flavors definition file also contains an entry for '$customFlavor'.
                """.trimMargin()
            }
        }

        val overwrittenFlavors = defaultConfig.flavorMap.map { (defaultFlavor, defaultSettings) ->
            val customOverrides = customConfig.flavorMap[defaultFlavor] ?: emptyMap()
            val overwrittenFlavor = defaultSettings.overwritingWith(defaultFlavor, customOverrides)
            defaultFlavor to overwrittenFlavor
        }.toMap()

        return NormalizedFlavorSettings(overwrittenFlavors)
    }

    enum class CustomizationProperty(val variableName: String) {
        CUSTOM_REPOSITORY("CUSTOM_REPOSITORY"),
        CUSTOM_FOLDER("CUSTOM_FOLDER"),
        CLIENT_FOLDER("CLIENT_FOLDER"),
        GIT_USER("GRGIT_USER"),
        GIT_PASSWORD("GRGIT_PASSWORD");
    }

    // Make it a sealed interface once we can use newer Kotlin versions
    sealed class CustomizationCoordinates {
        object NoCustomization : CustomizationCoordinates()

        data class Values(
            val customRepository: String,
            val customFolder: String,
            val clientFolder: String,
            val gitUser: String,
            val gitPassword: String
        ) : CustomizationCoordinates()

        companion object {
            fun fromProperties(properties: Properties): Values {
                fun requireCustomizationProperty(property: CustomizationProperty): String =
                    requireNotNull(properties.readCustomizationProperty(property)) {
                        "Missing ${property.variableName} property defined in local.properties or environment variable"
                    }

                return Values(
                    requireCustomizationProperty(CustomizationProperty.CUSTOM_REPOSITORY),
                    requireCustomizationProperty(CustomizationProperty.CUSTOM_FOLDER),
                    requireCustomizationProperty(CustomizationProperty.CLIENT_FOLDER),
                    requireCustomizationProperty(CustomizationProperty.GIT_USER),
                    requireCustomizationProperty(CustomizationProperty.GIT_PASSWORD),
                )
            }
        }
    }

    private fun Properties.readCustomizationProperty(property: CustomizationProperty): String? =
        System.getenv(property.variableName) ?: properties.getProperty(property.variableName)
}

data class NormalizedFlavorSettings(
    val flavorMap: Map<String, Map<String, Any?>>
)
