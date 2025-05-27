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

import org.ajoberstar.grgit.Credentials
import org.ajoberstar.grgit.Grgit
import java.io.File
import java.util.Properties

object Customization {

    /**
     * Where the custom properties git checkout coordinates are located.
     * These values can be set as environment variables, or in this file.
     * @see CustomizationGitProperty
     */
    private const val GIT_PROPERTIES_FILE_NAME = "local.properties"

    /**
     * The name of the directory used for storing customization files temporarily.
     * When checking out the customization repository, the files will be stored into this directory.
     */
    private const val CUSTOM_CHECKOUT_DIR_NAME = "custom"

    /**
     * The name of the folder which contains custom resources, that will overwrite the default resources in the app.
     * These resources will replace the flavor-specific resources from the Android app. Take the following structure:
     * ```
     * -custom/
     *      |-resources/
     *          |-mipmap/
     *          |    |-file1.png
     *          |-values/
     *          |    |-custom-strings.xml
     * ```
     * These files will me copied to every single flavor, like `prod` in this example:
     *
     * ```
     * -src/prod/res/
     *      |-resources/
     *          |-mipmap/
     *          |    |-file1.png
     *          |-values/
     *          |    |-custom-strings.xml
     * ```
     * Keep in mind that [Resource Merging](https://developer.android.com/studio/write/add-resources#resource_merging) can be used to our
     * advantage. For example, you don't need to copy all the strings from the `main/src/res/values`
     */
    private const val CUSTOM_RESOURCES_OVERRIDE_DIR_NAME = "resources"

    /**
     * The name of the JSON file for custom builds.
     * It is expected that custom builds provide this file in the root of the customization files.
     * The values in this file will overwrite the values in the default JSON configuration file.
     * @see DEFAULT_JSON_FILE_NAME
     */
    private const val CUSTOM_JSON_FILE_NAME = "custom-reloaded.json"

    /**
     * The JSON file name used for loading default build variables in various functions and classes.
     * This constant value is set to "default.json".
     * If configured, customiz builds will overwrite these values during build time.
     */
    private const val DEFAULT_JSON_FILE_NAME = "default.json"

    private val configurationFileImporter = ConfigurationFileImporter()
    private val properties = Properties().apply {
        val localProperties = File(GIT_PROPERTIES_FILE_NAME)
        if (localProperties.exists()) {
            load(localProperties.inputStream())
        }
    }

    /**
     * Basing all the work on the [rootDir], import configuration files
     * according to the specified [customizationOption].
     * Will attempt to read [CustomizationGitProperty] from environment variables
     * or [GIT_PROPERTIES_FILE_NAME] file if [customizationOption] is null.
     * @return the [BuildTimeConfiguration], result from the importing of configuration files.
     */
    fun getBuildtimeConfiguration(
        rootDir: File
    ): BuildTimeConfiguration {
        return if (isCustomizationEnabled()) {
            val customFile = getCustomisationFileFromGitProperties(rootDir)
            getBuildtimeConfiguration(rootDir, CustomizationOption.FromFile(customFile))
        } else {
            getBuildtimeConfiguration(rootDir, CustomizationOption.DefaultOnly)
        }
    }

    /**
     * Basing all the work on the [rootDir], import configuration files
     * according to the specified [customizationOption].
     * @return the [BuildTimeConfiguration], result from the importing of configuration files.
     */
    fun getBuildtimeConfiguration(
        rootDir: File,
        customizationOption: CustomizationOption
    ): BuildTimeConfiguration {

        val defaultConfigFile = File(rootDir, DEFAULT_JSON_FILE_NAME)
        val defaultConfig = configurationFileImporter.loadConfigsFromFile(defaultConfigFile)

        val normalizedFlavorSettings = when (customizationOption) {
            is CustomizationOption.DefaultOnly -> defaultConfig

            is CustomizationOption.FromFile -> getCustomBuildConfigs(
                defaultConfig,
                customizationOption.customJsonFile
            )
        }

        val resourcesOverrideDirectory = when(customizationOption){
            is CustomizationOption.DefaultOnly -> null
            is CustomizationOption.FromFile -> {
                File(customizationOption.customJsonFile.parentFile, CUSTOM_RESOURCES_OVERRIDE_DIR_NAME)
                    .takeIf { it.exists() }
            }
        }
        return BuildTimeConfiguration(normalizedFlavorSettings, resourcesOverrideDirectory)
    }

    /**
     * Uses environment variables or properties file to checkout a git repository
     * containing the customization file.
     * @see CustomizationGitProperty
     */
    private fun getCustomisationFileFromGitProperties(
        rootDir: File
    ): File {
        val customCheckoutDir = File(rootDir, CUSTOM_CHECKOUT_DIR_NAME)

        val customRepository: String = requireCustomizationProperty(CustomizationGitProperty.CUSTOM_REPOSITORY)
        val customBranch: String = requireCustomizationProperty(CustomizationGitProperty.CUSTOM_BRANCH)
        val customFolder: String = requireCustomizationProperty(CustomizationGitProperty.CUSTOM_FOLDER)
        val clientFolder: String = requireCustomizationProperty(CustomizationGitProperty.CLIENT_FOLDER)
        val gitUser: String = requireCustomizationProperty(CustomizationGitProperty.GIT_USER)
        val gitPassword: String = readCustomizationProperty(CustomizationGitProperty.GIT_PASSWORD).orEmpty()

        if (customCheckoutDir.exists()) {
            customCheckoutDir.deleteRecursively()
        }

        val credentials = Credentials(gitUser, gitPassword)
        Grgit.clone(mapOf(
            "dir" to customCheckoutDir,
            "uri" to customRepository,
            "refToCheckout" to customBranch,
            "credentials" to credentials
        ))

        return File(customCheckoutDir, "$customFolder/$clientFolder/$CUSTOM_JSON_FILE_NAME")
    }

    private fun getCustomBuildConfigs(
        defaultConfig: NormalizedFlavorSettings,
        customConfigFile: File
    ): NormalizedFlavorSettings {
        val customConfig = configurationFileImporter.loadConfigsFromFile(customConfigFile)

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

    /**
     * The expected values needed in order to check out the customization files.
     * The [variableName] can be set using environment variable or using the [GIT_PROPERTIES_FILE_NAME] file.
     */
    enum class CustomizationGitProperty(val variableName: String) {
        /**
         * The Git repository where the customization files are located.
         */
        CUSTOM_REPOSITORY("CUSTOM_REPOSITORY"),

        /**
         * The branch of the [CUSTOM_REPOSITORY] to check out.
         */
        CUSTOM_BRANCH("CUSTOM_BRANCH"),

        /**
         * The path to the root of the customization files within the [CUSTOM_REPOSITORY] files.
         */
        CUSTOM_FOLDER("CUSTOM_FOLDER"),

        /**
         * The name of the specific directory within the [CUSTOM_FOLDER] that contains the customization files for the build.
         */
        CLIENT_FOLDER("CLIENT_FOLDER"),

        /**
         * The git username for checking out the [CUSTOM_REPOSITORY].
         */
        GIT_USER("GRGIT_USER"),

        /**
         * The git password for checking out the [CUSTOM_REPOSITORY].
         */
        GIT_PASSWORD("GRGIT_PASSWORD"),
    }

    sealed class CustomizationOption {
        /**
         * Use only the [DEFAULT_JSON_FILE_NAME] file to load build variables.
         */
        object DefaultOnly : CustomizationOption()

        /**
         * Use the [DEFAULT_JSON_FILE_NAME] file, and overwrite its values
         * with the content of [customJsonFile].
         */
        data class FromFile(val customJsonFile: File) : CustomizationOption()
    }

    fun isCustomizationEnabled(): Boolean = readCustomizationProperty(CustomizationGitProperty.CUSTOM_REPOSITORY) != null

    private fun readCustomizationProperty(property: CustomizationGitProperty): String? =
        System.getenv(property.variableName) ?: properties.getProperty(property.variableName)

    private fun requireCustomizationProperty(property: CustomizationGitProperty): String =
        requireNotNull(readCustomizationProperty(property)) {
            "Missing ${property.variableName} property defined in $GIT_PROPERTIES_FILE_NAME or environment variable"
        }
}
