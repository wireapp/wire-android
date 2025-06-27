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

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotContain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.util.Properties

@RunWith(JUnit4::class)
class ConfigurationFileImporterTest {

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()
    private val configFile get() = File(folder.root, "someFile.json")

    private val importer get() = ConfigurationFileImporter()

    @Test
    fun givenDefaultValuesAndFlavorSpecifics_whenImportingFile_shouldOverwriteDefaultWithFlavorSpecific() {
        val keyName = "color"
        val overwrittenValue = "yellow"
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "strawberry": {},            
                    "apple": {},            
                    "banana": {
                        "$keyName": "$overwrittenValue"
                    }           
                },
                "$keyName": "red"
            }
            """.trimIndent()
        )

        val result = importer.loadConfigsFromFile(configFile)

        result.flavorMap["banana"]!![keyName] shouldBeEqualTo ConfigValue.BuildConfigValue(overwrittenValue)
    }

    @Test
    fun givenDefaultValuesAndFlavorSpecifics_whenImportingFile_shouldContainDefaultWhenNotOverwritten() {
        val keyName = "color"
        val defaultValue = "red"
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "strawberry": {},            
                    "apple": {},            
                    "banana": {
                        "$keyName": "yellow"
                    }           
                },
                "$keyName": "$defaultValue"
            }
            """.trimIndent()
        )

        val result = importer.loadConfigsFromFile(configFile)

        result.flavorMap["strawberry"]!![keyName] shouldBeEqualTo ConfigValue.BuildConfigValue(defaultValue)
        result.flavorMap["apple"]!![keyName] shouldBeEqualTo ConfigValue.BuildConfigValue(defaultValue)
    }

    @Test
    fun givenDefaultValuesAndFlavorSpecifics_whenImportingFile_shouldIncludeAllFlavors() {
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "strawberry": {},            
                    "apple": {},            
                    "banana": {
                        "color": "yellow"
                    }           
                },
                "color": "red"
            }
            """.trimIndent()
        )

        val result = importer.loadConfigsFromFile(configFile)

        result.flavorMap.keys.shouldContainAll(
            setOf("strawberry", "apple", "banana")
        )
    }

    @Test
    fun givenEnvironmentVariableInConfig_whenEnvVarSetInSystem_shouldResolveToSystemEnvValue() {
        // Arrange - Set up config with ENV: prefix
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "api_key": "ENV:DEV_ANALYTICS_APP_KEY"
                    }           
                },
                "default_key": "default_value"
            }
            """.trimIndent()
        )

        // Create local.properties fallback
        val localPropsFile = File(folder.root, "local.properties")
        localPropsFile.writeText("DEV_ANALYTICS_APP_KEY=fallback_from_local_properties")

        // Simulate environment variable by creating custom importer
        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("DEV_ANALYTICS_APP_KEY", "fallback_from_local_properties")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return if (varName == "DEV_ANALYTICS_APP_KEY") "value_from_system_env" else null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert - Should use system environment value (priority over local.properties)
        result.flavorMap["test"]!!["api_key"] shouldBeEqualTo ConfigValue.BuildConfigValue("value_from_system_env")
    }

    @Test
    fun givenEnvironmentVariableInConfig_whenEnvVarNotInSystemButInLocalProperties_shouldResolveToLocalPropertiesValue() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "api_key": "ENV:DEV_ANALYTICS_APP_KEY"
                    }           
                },
                "default_key": "default_value"
            }
            """.trimIndent()
        )

        // Create custom importer that simulates no system env but has local.properties
        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("DEV_ANALYTICS_APP_KEY", "value_from_local_properties")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null // Simulate no system environment variable
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert - Should use local.properties fallback
        result.flavorMap["test"]!!["api_key"] shouldBeEqualTo ConfigValue.BuildConfigValue("value_from_local_properties")
    }

    @Test
    fun givenEnvironmentVariableInConfig_whenEnvVarMissingFromBothSources_shouldThrowError() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "api_key": "ENV:DEV_ANALYTICS_APP_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        // Create custom importer that simulates no environment variable in either source
        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties() // Empty properties
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null // No system environment variables
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected IllegalStateException to be thrown")
        } catch (e: IllegalStateException) {
            e.message shouldBeEqualTo "Environment variable 'DEV_ANALYTICS_APP_KEY' is not set. Please define it in your environment or add it to local.properties file."
        }
    }

    @Test
    fun givenMixedConfigWithEnvironmentAndRegularValues_shouldResolveOnlyEnvironmentVariables() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "env_value": "ENV:DEV_ANALYTICS_APP_KEY",
                        "regular_value": "just_a_string",
                        "another_env": "ENV:DEV_GOOGLE_API_KEY"
                    }           
                },
                "default_regular": "default_string",
                "default_env": "ENV:PROD_ANALYTICS_APP_KEY"
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("DEV_ANALYTICS_APP_KEY", "resolved_test_value")
                props.setProperty("DEV_GOOGLE_API_KEY", "resolved_another_value")
                props.setProperty("PROD_ANALYTICS_APP_KEY", "resolved_default_value")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null // Use local.properties for all
            }
    }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert
        val testFlavor = result.flavorMap["test"]!!
        testFlavor["env_value"] shouldBeEqualTo ConfigValue.BuildConfigValue("resolved_test_value")
        testFlavor["regular_value"] shouldBeEqualTo ConfigValue.BuildConfigValue("just_a_string") // Should remain unchanged
        testFlavor["another_env"] shouldBeEqualTo ConfigValue.BuildConfigValue("resolved_another_value")
        testFlavor["default_regular"] shouldBeEqualTo ConfigValue.BuildConfigValue("default_string") // Should remain unchanged
        testFlavor["default_env"] shouldBeEqualTo ConfigValue.BuildConfigValue("resolved_default_value")
    }

    @Test
    fun givenNestedEnvironmentVariables_shouldResolveRecursively() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "nested_config": {
                            "api_key": "ENV:DEV_ANALYTICS_APP_KEY",
                            "server_url": "ENV:PROD_ANALYTICS_APP_KEY"
                        }
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("DEV_ANALYTICS_APP_KEY", "resolved_nested_api")
                props.setProperty("PROD_ANALYTICS_APP_KEY", "https://resolved-server.com")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert
        val testFlavor = result.flavorMap["test"]!!
        @Suppress("UNCHECKED_CAST")
        val nestedConfigValue = testFlavor["nested_config"] as ConfigValue.BuildConfigValue
        val nestedConfig = nestedConfigValue.value as Map<String, Any?>
        nestedConfig["api_key"] shouldBeEqualTo "resolved_nested_api"
        nestedConfig["server_url"] shouldBeEqualTo "https://resolved-server.com"
    }

    @Test
    fun givenEnvironmentVariableWithoutENVPrefix_shouldNotResolve() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "normal_value": "DEV_ANALYTICS_APP_KEY",
                        "env_value": "ENV:DEV_ANALYTICS_APP_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("DEV_ANALYTICS_APP_KEY", "resolved_value")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert
        val testFlavor = result.flavorMap["test"]!!
        testFlavor["normal_value"] shouldBeEqualTo ConfigValue.BuildConfigValue("DEV_ANALYTICS_APP_KEY") // Should remain as literal string
        testFlavor["env_value"] shouldBeEqualTo ConfigValue.BuildConfigValue("resolved_value") // Should be resolved
    }

    @Test
    fun givenAllowedEnvironmentVariable_whenResolvingConfig_shouldResolveSuccessfully() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "analytics_key": "ENV:PROD_ANALYTICS_APP_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("PROD_ANALYTICS_APP_KEY", "allowed_value")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert
        result.flavorMap["test"]!!["analytics_key"] shouldBeEqualTo ConfigValue.BuildConfigValue("allowed_value")
    }

    @Test
    fun givenDisallowedEnvironmentVariable_whenResolvingConfig_shouldThrowSecurityError() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "malicious_field": "ENV:AWS_SECRET_ACCESS_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties()
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            e.message?.shouldContain("SECURITY VIOLATION")
            e.message?.shouldContain("AWS_SECRET_ACCESS_KEY")
            e.message?.shouldContain("not in the allowed list")
            e.message?.shouldContain("PROD_ANALYTICS_APP_KEY")
        }
    }

    @Test
    fun givenMultipleDisallowedVariables_whenResolvingConfig_shouldFailOnFirstViolation() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "field1": "ENV:DATABASE_PASSWORD",
                        "field2": "ENV:GITHUB_TOKEN",
                        "field3": "ENV:AWS_SECRET_ACCESS_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties()
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            // Should fail on the first disallowed variable encountered
            e.message?.shouldContain("SECURITY VIOLATION")
            e.message?.shouldContain("not in the allowed list")
        }
    }

    @Test
    fun givenMixedAllowedAndDisallowedVariables_whenResolvingConfig_shouldFailOnDisallowedVariable() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "allowed_field": "ENV:PROD_ANALYTICS_APP_KEY",
                        "disallowed_field": "ENV:SECRET_API_KEY",
                        "normal_field": "normal_value"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("PROD_ANALYTICS_APP_KEY", "allowed_value")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            e.message?.shouldContain("SECRET_API_KEY")
            e.message?.shouldContain("not in the allowed list")
        }
    }

    @Test
    fun givenNestedDisallowedEnvironmentVariable_whenResolvingConfig_shouldThrowSecurityError() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "nested_config": {
                            "safe_value": "normal_string",
                            "malicious_value": "ENV:DATABASE_URL"
                        }
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties()
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            e.message?.shouldContain("DATABASE_URL")
            e.message?.shouldContain("not in the allowed list")
        }
    }

    @Test
    fun givenAllAllowedEnvironmentVariables_whenResolvingConfig_shouldResolveAllSuccessfully() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "prod_analytics_key": "ENV:PROD_ANALYTICS_APP_KEY",
                        "dev_analytics_key": "ENV:DEV_ANALYTICS_APP_KEY",
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("PROD_ANALYTICS_APP_KEY", "prod_key")
                props.setProperty("DEV_ANALYTICS_APP_KEY", "dev_key")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert
        val testFlavor = result.flavorMap["test"]!!
        testFlavor["prod_analytics_key"] shouldBeEqualTo ConfigValue.BuildConfigValue("prod_key")
        testFlavor["dev_analytics_key"] shouldBeEqualTo ConfigValue.BuildConfigValue("dev_key")
    }

    // === EDGE CASE SECURITY TESTS ===

    @Test
    fun givenEmptyEnvironmentVariableName_whenResolvingConfig_shouldThrowSecurityError() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "empty_var": "ENV:"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties()
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            e.message?.shouldContain("SECURITY VIOLATION")
            e.message?.shouldContain("not in the allowed list")
        }
    }

    @Test
    fun givenEnvironmentVariableWithSpecialCharacters_whenNotInAllowlist_shouldThrowSecurityError() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "special_var": "ENV:SECRET_KEY@#$%"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties()
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            e.message?.shouldContain("SECRET_KEY@#$%")
            e.message?.shouldContain("not in the allowed list")
        }
    }

    @Test
    fun givenCaseSensitiveEnvironmentVariable_whenNotExactMatch_shouldThrowSecurityError() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "case_var": "ENV:prod_analytics_app_key"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties()
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            e.message?.shouldContain("prod_analytics_app_key")
            e.message?.shouldContain("not in the allowed list")
        }
    }

    @Test
    fun givenEnvironmentVariableWithWhitespace_whenNotInAllowlist_shouldThrowSecurityError() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "whitespace_var": "ENV: PROD_ANALYTICS_APP_KEY "
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties()
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            e.message?.shouldContain(" PROD_ANALYTICS_APP_KEY ")
            e.message?.shouldContain("not in the allowed list")
        }
    }

    // === ATTACK VECTOR PREVENTION TESTS ===

    @Test
    fun givenAttemptToBypassAllowlistWithSimilarName_whenResolvingConfig_shouldThrowSecurityError() {
        val bypassAttempts = listOf(
            "PROD_ANALYTICS_APP_KEY_EXTRA",  // Adding suffix
            "XPROD_ANALYTICS_APP_KEY",       // Adding prefix
            "PROD_ANALYTICS_APP_KEYS",       // Pluralization
            "PROD_ANALYTICS_APP_KE"          // Truncation
        )

        bypassAttempts.forEach { varName ->
            // Arrange
            configFile.writeText(
                """
                {
                "${ConfigurationFileImporter.KEY_FLAVORS}": {
                        "test": {
                            "bypass_var": "ENV:$varName"
                        }           
                    }
                }
                """.trimIndent()
            )

            val customImporter = object : ConfigurationFileImporter() {
                override fun loadLocalProperties(): Properties {
                    return Properties()
                }
                
                override fun getSystemEnv(varName: String): String? {
                    return null
                }
            }

            // Act & Assert
            try {
                customImporter.loadConfigsFromFile(configFile)
                throw AssertionError("Expected security violation for bypass attempt: $varName")
            } catch (e: IllegalStateException) {
                e.message?.shouldContain(varName)
                e.message?.shouldContain("not in the allowed list")
            }
        }
    }

    @Test
    fun givenCommonlyTargetedSecrets_whenNotInAllowlist_shouldThrowSecurityError() {
        val maliciousVariables = listOf(
            "AWS_SECRET_ACCESS_KEY",
            "DATABASE_PASSWORD", 
            "GITHUB_TOKEN",
            "STRIPE_SECRET_KEY",
            "JWT_SECRET",
            "API_SECRET",
            "PRIVATE_KEY",
            "MASTER_KEY"
        )

        maliciousVariables.forEach { varName ->
            // Arrange
            configFile.writeText(
                """
                {
                "${ConfigurationFileImporter.KEY_FLAVORS}": {
                        "test": {
                            "malicious_field": "ENV:$varName"
                        }           
                    }
                }
                """.trimIndent()
            )

            val customImporter = object : ConfigurationFileImporter() {
                override fun loadLocalProperties(): Properties {
                    return Properties()
                }
                
                override fun getSystemEnv(varName: String): String? {
                    return null
                }
            }

            // Act & Assert
            try {
                customImporter.loadConfigsFromFile(configFile)
                throw AssertionError("Expected security violation for malicious variable: $varName")
            } catch (e: IllegalStateException) {
                e.message?.shouldContain(varName)
                e.message?.shouldContain("SECURITY VIOLATION")
                e.message?.shouldContain("not in the allowed list")
            }
        }
    }

    @Test
    fun givenEnvironmentVariableInjectionAttempt_whenNotInAllowlist_shouldThrowSecurityError() {
        val injectionAttempts = listOf(
            "SECRET_KEY;echo 'pwned'",     // Command injection attempt
            "SECRET_KEY\${PATH}",          // Variable expansion attempt
            "SECRET_KEY`whoami`",          // Command substitution attempt
            "SECRET_KEY||rm -rf /",        // Command chaining attempt
            "SECRET_KEY$(id)"              // Command substitution attempt
        )

        injectionAttempts.forEach { varName ->
            // Arrange
            configFile.writeText(
                """
                {
                "${ConfigurationFileImporter.KEY_FLAVORS}": {
                        "test": {
                            "injection_var": "ENV:$varName"
                        }           
                    }
                }
                """.trimIndent()
            )

            val customImporter = object : ConfigurationFileImporter() {
                override fun loadLocalProperties(): Properties {
                    return Properties()
                }
                
                override fun getSystemEnv(varName: String): String? {
                    return null
                }
            }

            // Act & Assert
            try {
                customImporter.loadConfigsFromFile(configFile)
                throw AssertionError("Expected security violation for injection attempt: $varName")
            } catch (e: IllegalStateException) {
                e.message?.shouldContain("SECURITY VIOLATION")
                e.message?.shouldContain("not in the allowed list")
            }
        }
    }

    // === CONFIGURATION VALIDATION SECURITY TESTS ===

    @Test
    fun givenMalformedEnvPrefix_whenResolvingConfig_shouldNotResolve() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "malformed1": "env:PROD_ANALYTICS_APP_KEY",
                        "malformed2": "Env:PROD_ANALYTICS_APP_KEY",
                        "malformed3": "ENV_PROD_ANALYTICS_APP_KEY",
                        "correct": "normal_value"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("PROD_ANALYTICS_APP_KEY", "resolved_value")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert - malformed prefixes should be treated as literal strings
        val testFlavor = result.flavorMap["test"]!!
        testFlavor["malformed1"] shouldBeEqualTo ConfigValue.BuildConfigValue("env:PROD_ANALYTICS_APP_KEY")  // Not resolved
        testFlavor["malformed2"] shouldBeEqualTo ConfigValue.BuildConfigValue("Env:PROD_ANALYTICS_APP_KEY")  // Not resolved
        testFlavor["malformed3"] shouldBeEqualTo ConfigValue.BuildConfigValue("ENV_PROD_ANALYTICS_APP_KEY")  // Not resolved
        testFlavor["correct"] shouldBeEqualTo ConfigValue.BuildConfigValue("normal_value")
    }

    @Test
    fun givenEnvPrefixInDifferentCase_whenResolvingConfig_shouldNotResolve() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "lowercase": "env:PROD_ANALYTICS_APP_KEY",
                        "mixed_case": "Env:PROD_ANALYTICS_APP_KEY",
                        "correct_case": "ENV:PROD_ANALYTICS_APP_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("PROD_ANALYTICS_APP_KEY", "resolved_value")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert
        val testFlavor = result.flavorMap["test"]!!
        testFlavor["lowercase"] shouldBeEqualTo ConfigValue.BuildConfigValue("env:PROD_ANALYTICS_APP_KEY")     // Not resolved
        testFlavor["mixed_case"] shouldBeEqualTo ConfigValue.BuildConfigValue("Env:PROD_ANALYTICS_APP_KEY")   // Not resolved
        testFlavor["correct_case"] shouldBeEqualTo ConfigValue.BuildConfigValue("resolved_value")              // Resolved
    }

    @Test
    fun givenDoubleEnvPrefix_whenResolvingConfig_shouldThrowSecurityError() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "double_prefix": "ENV:ENV:MALICIOUS_VAR"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties()
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            e.message?.shouldContain("ENV:MALICIOUS_VAR")
            e.message?.shouldContain("not in the allowed list")
        }
    }

    @Test
    fun givenEnvPrefixWithNoColon_whenResolvingConfig_shouldNotResolve() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "no_colon": "ENVPROD_ANALYTICS_APP_KEY",
                        "space_instead": "ENV PROD_ANALYTICS_APP_KEY",
                        "correct": "ENV:PROD_ANALYTICS_APP_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("PROD_ANALYTICS_APP_KEY", "resolved_value")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert
        val testFlavor = result.flavorMap["test"]!!
        testFlavor["no_colon"] shouldBeEqualTo ConfigValue.BuildConfigValue("ENVPROD_ANALYTICS_APP_KEY")      // Not resolved
        testFlavor["space_instead"] shouldBeEqualTo ConfigValue.BuildConfigValue("ENV PROD_ANALYTICS_APP_KEY") // Not resolved  
        testFlavor["correct"] shouldBeEqualTo ConfigValue.BuildConfigValue("resolved_value")                   // Resolved
    }

    // === ERROR MESSAGE VALIDATION TESTS ===

    @Test
    fun givenSecurityViolation_whenErrorThrown_shouldContainHelpfulMessage() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "malicious_field": "ENV:AWS_SECRET_ACCESS_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties()
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            // Verify error message contains helpful information
            e.message?.shouldContain("SECURITY VIOLATION")
            e.message?.shouldContain("AWS_SECRET_ACCESS_KEY")
            e.message?.shouldContain("not in the allowed list")
            e.message?.shouldContain("malicious JSON injection attacks")
            e.message?.shouldContain("If this is a legitimate environment variable")
            e.message?.shouldContain("Add 'AWS_SECRET_ACCESS_KEY' to ALLOWED_ENV_VARS")
        }
    }

    @Test
    fun givenSecurityViolation_whenErrorThrown_shouldListAllowedVariables() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "malicious_field": "ENV:UNAUTHORIZED_VAR"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties()
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            // Verify error message lists allowed variables for reference
            e.message?.shouldContain("Allowed variables:")
            e.message?.shouldContain("PROD_ANALYTICS_APP_KEY")
            e.message?.shouldContain("DEV_ANALYTICS_APP_KEY")
        }
    }

    @Test
    fun givenSecurityViolation_whenErrorThrown_shouldNotLeakSensitiveInformation() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "malicious_field": "ENV:SECRET_DATABASE_PASSWORD"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                // Simulate having sensitive data in properties (should not be leaked)
                props.setProperty("INTERNAL_SECRET", "super_secret_value")
                props.setProperty("DATABASE_CONNECTION", "secret_connection_string")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                // Simulate environment having sensitive data (should not be leaked)
                return when (varName) {
                    "PATH" -> "/usr/bin:/bin"
                    "USER" -> "admin"
                    else -> null
                }
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            // Verify error message doesn't leak sensitive information
            e.message?.shouldNotContain("super_secret_value")
            e.message?.shouldNotContain("secret_connection_string")
            e.message?.shouldNotContain("/usr/bin:/bin")
            e.message?.shouldNotContain("admin")
            
            // But should contain security guidance
            e.message?.shouldContain("SECRET_DATABASE_PASSWORD")
            e.message?.shouldContain("SECURITY VIOLATION")
        }
    }

    @Test
    fun givenMissingEnvironmentVariable_whenErrorThrown_shouldProvideGuidance() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "analytics_key": "ENV:PROD_ANALYTICS_APP_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties() // Empty - variable not found
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null // Not in system environment either
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected missing variable exception")
        } catch (e: IllegalStateException) {
            // Verify helpful guidance for missing variables
            e.message?.shouldContain("Environment variable 'PROD_ANALYTICS_APP_KEY' is not set")
            e.message?.shouldContain("Please define it in your environment")
            e.message?.shouldContain("add it to local.properties file")
        }
    }

    // === MANIFEST PLACEHOLDER TESTS ===

    @Test
    fun givenManifestPrefixWithAllowedEnvironmentVariable_shouldResolveToManifestPlaceholderValue() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "analytics_key": "MANIFEST:PROD_ANALYTICS_APP_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("PROD_ANALYTICS_APP_KEY", "resolved_analytics_key")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert
        val configValue = result.flavorMap["test"]!!["analytics_key"]!!
        configValue shouldBeEqualTo ConfigValue.ManifestPlaceholderValue("resolved_analytics_key")
        (configValue as ConfigValue.ManifestPlaceholderValue).value shouldBeEqualTo "resolved_analytics_key"
    }

    @Test
    fun givenManifestPrefixWithUnallowedEnvironmentVariables_shouldThrowSecurityError() {
        // Arrange - MANIFEST: values are always treated as environment variables
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "api_key": "MANIFEST:INVALID_API_KEY",
                        "feature_flag": "MANIFEST:UNAUTHORIZED_VAR"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties()
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            e.message?.shouldContain("SECURITY VIOLATION")
            e.message?.shouldContain("not in the allowed list")
        }
    }

    @Test
    fun givenManifestPrefixWithSimpleSyntax_shouldResolveEnvironmentVariable() {
        // Arrange - MANIFEST: always means environment variable, no ENV: prefix needed
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "analytics_key": "MANIFEST:PROD_ANALYTICS_APP_KEY",
                        "google_key": "MANIFEST:DEV_GOOGLE_API_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("PROD_ANALYTICS_APP_KEY", "analytics_value")
                props.setProperty("DEV_GOOGLE_API_KEY", "google_value")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert
        val testFlavor = result.flavorMap["test"]!!
        val analyticsValue = testFlavor["analytics_key"]!!
        val googleValue = testFlavor["google_key"]!!
        
        analyticsValue shouldBeEqualTo ConfigValue.ManifestPlaceholderValue("analytics_value")
        googleValue shouldBeEqualTo ConfigValue.ManifestPlaceholderValue("google_value")
        
        (analyticsValue as ConfigValue.ManifestPlaceholderValue).value shouldBeEqualTo "analytics_value"
        (googleValue as ConfigValue.ManifestPlaceholderValue).value shouldBeEqualTo "google_value"
    }

    @Test
    fun givenManifestPrefixWithUnallowedEnvironmentVariable_shouldThrowSecurityError() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "malicious_field": "MANIFEST:AWS_SECRET_ACCESS_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                return Properties()
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act & Assert
        try {
            customImporter.loadConfigsFromFile(configFile)
            throw AssertionError("Expected security violation exception")
        } catch (e: IllegalStateException) {
            e.message?.shouldContain("SECURITY VIOLATION")
            e.message?.shouldContain("AWS_SECRET_ACCESS_KEY")
            e.message?.shouldContain("not in the allowed list")
        }
    }

    @Test
    fun givenManifestPrefixWithSystemEnvironmentPriority_shouldPreferSystemOverLocal() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "analytics_key": "MANIFEST:PROD_ANALYTICS_APP_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("PROD_ANALYTICS_APP_KEY", "local_value")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return if (varName == "PROD_ANALYTICS_APP_KEY") "system_value" else null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert - Should prefer system environment over local.properties
        val configValue = result.flavorMap["test"]!!["analytics_key"]
        (configValue as ConfigValue.ManifestPlaceholderValue).value shouldBeEqualTo "system_value"
    }

    @Test
    fun givenMixedBuildConfigAndManifestValues_shouldCreateCorrectConfigValueTypes() {
        // Arrange - MANIFEST: values are always environment variables now
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "build_config_env": "ENV:PROD_ANALYTICS_APP_KEY",
                        "build_config_literal": "literal_value",
                        "manifest_env": "MANIFEST:PROD_ANALYTICS_APP_KEY",
                        "manifest_another_env": "MANIFEST:DEV_GOOGLE_API_KEY"
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("PROD_ANALYTICS_APP_KEY", "analytics_value")
                props.setProperty("DEV_GOOGLE_API_KEY", "google_value")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert
        val testFlavor = result.flavorMap["test"]!!
        
        // BuildConfig values
        val buildConfigEnv = testFlavor["build_config_env"]
        val buildConfigLiteral = testFlavor["build_config_literal"]
        buildConfigEnv shouldBeEqualTo ConfigValue.BuildConfigValue("analytics_value")
        buildConfigLiteral shouldBeEqualTo ConfigValue.BuildConfigValue("literal_value")
        
        // Manifest values (both are environment variables)
        val manifestEnv = testFlavor["manifest_env"]
        val manifestAnotherEnv = testFlavor["manifest_another_env"]
        manifestEnv shouldBeEqualTo ConfigValue.ManifestPlaceholderValue("analytics_value")
        manifestAnotherEnv shouldBeEqualTo ConfigValue.ManifestPlaceholderValue("google_value")
    }

    @Test
    fun givenNestedManifestValues_shouldResolveRecursivelyButFlattenToRawValues() {
        // Arrange
        configFile.writeText(
            """
            {
            "${ConfigurationFileImporter.KEY_FLAVORS}": {
                    "test": {
                        "nested_config": {
                            "manifest_key": "MANIFEST:PROD_ANALYTICS_APP_KEY",
                            "regular_key": "regular_value",
                            "build_config_key": "ENV:DEV_ANALYTICS_APP_KEY"
                        }
                    }           
                }
            }
            """.trimIndent()
        )

        val customImporter = object : ConfigurationFileImporter() {
            override fun loadLocalProperties(): Properties {
                val props = Properties()
                props.setProperty("PROD_ANALYTICS_APP_KEY", "prod_key")
                props.setProperty("DEV_ANALYTICS_APP_KEY", "dev_key")
                return props
            }
            
            override fun getSystemEnv(varName: String): String? {
                return null
            }
        }

        // Act
        val result = customImporter.loadConfigsFromFile(configFile)

        // Assert
        val testFlavor = result.flavorMap["test"]!!
        val nestedConfigValue = testFlavor["nested_config"]
        
        // Nested maps should be BuildConfigValue containing raw values
        nestedConfigValue shouldBeEqualTo ConfigValue.BuildConfigValue(mapOf(
            "manifest_key" to "prod_key",
            "regular_key" to "regular_value", 
            "build_config_key" to "dev_key"
        ))
    }
}
