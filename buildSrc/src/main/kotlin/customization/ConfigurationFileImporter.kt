/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package customization

import groovy.json.JsonSlurper
import java.io.File
import java.util.Properties

open class ConfigurationFileImporter {

    private val jsonReader = JsonSlurper()
    private val localProperties: Properties by lazy { loadLocalProperties() }

    /**
     * Security allowlist of environment variables that are permitted to be resolved.
     * This prevents malicious JSON injection attacks that could extract arbitrary
     * environment variables (AWS keys, database passwords, etc.) into the APK.
     * 
     * To add a new environment variable:
     * 1. Add it to this allowlist
     * 2. Define it in FeatureConfigs.kt if it's a new config key
     * 3. Document its purpose and security implications
     */
    private val ALLOWED_ENV_VARS = setOf(
        // Analytics configuration
        "PROD_ANALYTICS_APP_KEY",
        "DEV_ANALYTICS_APP_KEY",

        // google API keys
        "PROD_GOOGLE_API_KEY",
        "DEV_GOOGLE_API_KEY",

        // Add new environment variables here with comments explaining their purpose
        // Example: "PROD_API_KEY",  // Main API key for production
    )

    fun loadConfigsFromFile(file: File): NormalizedFlavorSettings {
        val importedMap = jsonReader.parseText(file.readText())
        require(importedMap is Map<*, *>) {
            "Imported file '$file' isn't recognised as a JSON object"
        }
        importedMap as? Map<String, *> ?: throw IllegalArgumentException(
            "Imported file '$file' could not be properly deserialized into a JSON object"
        )
        val resolvedMap = resolveEnvironmentVariables(importedMap)
        return normalizeFlavorOverrides(resolvedMap)
    }

    /**
     * Recursively resolves environment variables in the configuration map.
     * Values starting with "ENV:" are treated as required environment variable references
     * Values starting with "MANIFEST:" indicate the value should go to AndroidManifest.xml
     * and resolved using this fallback chain:
     * 1. Security validation against allowlist
     * 2. System environment variables (for CI)
     * 3. local.properties file (for local development)
     */
    private fun resolveEnvironmentVariables(configs: Map<String, Any?>, preserveConfigValues: Boolean = false): Map<String, ConfigValue> {
        return configs.mapValues { (key, value) ->
            when (value) {
                is String -> {
                    when {
                        value.startsWith("MANIFEST:") -> {
                            val innerValue = value.removePrefix("MANIFEST:")
                            val resolvedValue = resolveStringValue(innerValue)
                            ConfigValue.ManifestPlaceholderValue(resolvedValue)
                        }

                        value.startsWith("ENV:") -> {
                            val envVarName = value.removePrefix("ENV:")
                            val resolvedValue = resolveEnvironmentVariable(envVarName, isOptional = false)
                            ConfigValue.BuildConfigValue(resolvedValue)
                        }

                        else -> ConfigValue.BuildConfigValue(value)
                    }
                }
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val resolvedMap = resolveEnvironmentVariables(value as Map<String, Any?>, preserveConfigValues = (key == KEY_FLAVORS))
                    if (preserveConfigValues) {
                        // For flavors map, preserve ConfigValue types
                        ConfigValue.BuildConfigValue(resolvedMap)
                    } else {
                        // For other nested maps, convert back to raw values
                        val rawMap = resolvedMap.mapValues { (_, v) -> v.getRawValue() }
                        ConfigValue.BuildConfigValue(rawMap)
                    }
                }
                else -> ConfigValue.BuildConfigValue(value)
            }
        }
    }

    /**
     * Resolves a string value for MANIFEST: prefix handling.
     * For MANIFEST: prefix, the value after the colon is ALWAYS treated as an environment variable name.
     * 
     * @param value The string value to resolve (what comes after MANIFEST:)
     * @return The resolved environment variable value
     */
    private fun resolveStringValue(value: String): Any? {
        return when {

            value.startsWith("ENV:") -> {
                val envVarName = value.removePrefix("ENV:")
                resolveEnvironmentVariable(envVarName, isOptional = false)
            }
            else -> {
                // For MANIFEST: prefix, always treat as environment variable name
                resolveEnvironmentVariable(value, isOptional = false)
            }
        }
    }

    /**
     * Resolves a single environment variable with security validation and optional behavior.
     * 
     * @param envVarName The name of the environment variable to resolve
     * @param isOptional If true, returns null when variable is not found; if false, throws error
     * @return The resolved value or null (for optional variables only)
     */
    private fun resolveEnvironmentVariable(envVarName: String, isOptional: Boolean): String? {
        // SECURITY CHECK: Only allow predefined environment variables
        if (!ALLOWED_ENV_VARS.contains(envVarName)) {
            error(
                """
                |SECURITY VIOLATION: Environment variable '$envVarName' is not in the allowed list.
                |
                |This is a security measure to prevent malicious JSON injection attacks that could 
                |extract arbitrary secrets (AWS keys, database passwords, etc.) into the APK.
                |
                |If this is a legitimate environment variable:
                |1. Add '$envVarName' to ALLOWED_ENV_VARS in ConfigurationFileImporter.kt
                |2. Document its purpose and security implications
                |3. Ensure it doesn't contain sensitive information that shouldn't be in the APK
                |
                |Allowed variables: ${ALLOWED_ENV_VARS.sorted()}
                """.trimMargin()
            )
        }
        
        // Try system environment first (for CI)
        val resolvedValue = getSystemEnv(envVarName) 
            // Fallback to local.properties (for local dev)
            ?: localProperties.getProperty(envVarName)
        
        return when {
            resolvedValue != null -> resolvedValue
            isOptional -> null  // Return null for optional variables
            else -> error("Environment variable '$envVarName' is not set. Please define it in your environment or add it to local.properties file.")
        }
    }

    /**
     * Gets system environment variable. Can be overridden for testing.
     */
    protected open fun getSystemEnv(varName: String): String? {
        return System.getenv(varName)
    }

    /**
     * Loads local.properties file as a fallback for environment variables.
     * Returns empty Properties if file doesn't exist.
     */
    protected open fun loadLocalProperties(): Properties {
        val properties = Properties()
        val localPropsFile = File("local.properties")
        if (localPropsFile.exists()) {
            try {
                localPropsFile.inputStream().use { properties.load(it) }
            } catch (e: Exception) {
                println("Warning: Could not load local.properties: ${e.message}")
            }
        }
        return properties
    }

    // Public accessor for BuildTimeConfiguration
    fun resolveEnvironmentVariablePublic(envVarName: String, isOptional: Boolean): String? {
        return resolveEnvironmentVariable(envVarName, isOptional)
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
    private fun normalizeFlavorOverrides(configs: Map<String, ConfigValue>): NormalizedFlavorSettings {
        val flavorSets = configs[KEY_FLAVORS]
        requireNotNull(flavorSets) {
            "Can't normalize map as it does not contain a flavor list"
        }
        // Handle flavors map that preserves ConfigValue types
        val flavorSetsValue = if (flavorSets is ConfigValue.BuildConfigValue && flavorSets.value is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            flavorSets.value as Map<String, Map<String, ConfigValue>>
        } else {
            // Fallback to old behavior for compatibility
            val rawValue = flavorSets.getRawValue()
            require(rawValue is Map<*, *>) {
                "The 'flavors' map entry should contain another map of config overrides"
            }
            @Suppress("UNCHECKED_CAST")
            rawValue as Map<String, Map<String, Any?>>
        }
        
        val topLevelConfigs = configs.filter { it.key != KEY_FLAVORS }

        val normalizedMap = flavorSetsValue.map { (flavorName, overrides) ->
            require(flavorName is String) {
                "The flavor $flavorName is not named using a valid String"
            }
            val overwrittenTopLevelConfigs = if (overrides is Map<*, *> && 
                overrides.values.firstOrNull() is ConfigValue) {
                // New path: flavors contain ConfigValue objects
                @Suppress("UNCHECKED_CAST")
                topLevelConfigs.plus(overrides as Map<String, ConfigValue>)
            } else {
                // Old path: flavors contain raw values that need processing
                require(overrides is Map<*, *>) {
                    "The entry '$flavorName' is not a valid map containing config overrides"
                }
                topLevelConfigs.overwritingWith(flavorName, overrides)
            }
            flavorName to overwrittenTopLevelConfigs
        }.toMap()

        return NormalizedFlavorSettings(normalizedMap)
    }

    internal companion object {
        const val KEY_FLAVORS = "flavors"
    }
}
