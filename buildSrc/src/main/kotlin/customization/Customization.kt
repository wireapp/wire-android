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
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object Customization {

    /**
     * Where the custom properties git checkout coordinates are located.
     * These values can be set as environment variables, or in this file.
     * @see CustomizationGitProperty
     */
    private const val GIT_PROPERTIES_FILE_NAME = "local.properties"

    /**
     * Maximum time allowed for cloning the customization repository.
     * Grgit/JGit does not expose a timeout for this operation, and on some setups (e.g. SSH auth
     * that needs a host-key confirmation or a passphrase, with no terminal attached to prompt for
     * it) the clone can block indefinitely. Bounding it here turns a silent, indefinite Gradle sync
     * hang into a clear failure.
     */
    private const val CLONE_TIMEOUT_SECONDS = 120L

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

    /**
     * Loads [GIT_PROPERTIES_FILE_NAME], resolved relative to [rootDir] rather than the JVM's
     * working directory, which is not guaranteed to be the project root (e.g. when Gradle is
     * launched by Android Studio). Loaded fresh on every call rather than cached, since this is
     * an `object` and a cached value would otherwise survive for the lifetime of the Gradle
     * daemon, ignoring later edits to `local.properties` until the daemon is restarted.
     */
    private fun loadProperties(rootDir: File): Properties = Properties().apply {
        val localProperties = File(rootDir, GIT_PROPERTIES_FILE_NAME)
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
        return if (isCustomizationEnabled(rootDir)) {
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
     * Uses environment variables or properties file to obtain the customization file, either by
     * checking out a git repository or by copying a local folder, depending on which of
     * [CustomizationGitProperty.CUSTOM_REPOSITORY] or [CustomizationGitProperty.CUSTOM_LOCAL_FOLDER] is set.
     * [CustomizationGitProperty.CUSTOM_FOLDER] and [CustomizationGitProperty.CLIENT_FOLDER] are only
     * needed (and only read) for the git repository case: [CustomizationGitProperty.CUSTOM_LOCAL_FOLDER]
     * is expected to point directly at the client build folder.
     * @see CustomizationGitProperty
     */
    private fun getCustomisationFileFromGitProperties(
        rootDir: File
    ): File {
        val properties = loadProperties(rootDir)
        val customCheckoutDir = File(rootDir, CUSTOM_CHECKOUT_DIR_NAME)

        val customLocalFolder = readCustomizationProperty(properties, CustomizationGitProperty.CUSTOM_LOCAL_FOLDER)
        val customRepository = readCustomizationProperty(properties, CustomizationGitProperty.CUSTOM_REPOSITORY)
        require(customLocalFolder == null || customRepository == null) {
            "${CustomizationGitProperty.CUSTOM_LOCAL_FOLDER.variableName} and ${CustomizationGitProperty.CUSTOM_REPOSITORY.variableName} " +
                "are mutually exclusive: only one customization source can be used at a time."
        }

        return if (customLocalFolder != null) {
            copyLocalCustomizationFolder(rootDir, customLocalFolder, customCheckoutDir)
            File(customCheckoutDir, CUSTOM_JSON_FILE_NAME)
        } else {
            val customFolder: String = requireCustomizationProperty(properties, CustomizationGitProperty.CUSTOM_FOLDER)
            val clientFolder: String = requireCustomizationProperty(properties, CustomizationGitProperty.CLIENT_FOLDER)
            cloneCustomizationRepository(properties, customCheckoutDir)
            File(customCheckoutDir, "$customFolder/$clientFolder/$CUSTOM_JSON_FILE_NAME")
        }
    }

    /**
     * Copies [customLocalFolderPath] (resolved relative to [rootDir] if not absolute) into
     * [customCheckoutDir], as a local alternative to [cloneCustomizationRepository].
     */
    private fun copyLocalCustomizationFolder(
        rootDir: File,
        customLocalFolderPath: String,
        customCheckoutDir: File
    ) {
        val sourceDir = File(customLocalFolderPath).let { if (it.isAbsolute) it else File(rootDir, customLocalFolderPath) }
        require(sourceDir.isDirectory) {
            "${CustomizationGitProperty.CUSTOM_LOCAL_FOLDER.variableName} '$customLocalFolderPath' " +
                "(resolved to '${sourceDir.absolutePath}') does not exist or is not a directory"
        }

        println(">> Customization local folder specified: copying '${sourceDir.absolutePath}' into '${customCheckoutDir.absolutePath}'...")

        if (customCheckoutDir.exists()) {
            customCheckoutDir.deleteRecursively()
        }
        sourceDir.copyRecursively(customCheckoutDir, overwrite = true)

        println(">> Customization folder copied successfully into '${customCheckoutDir.absolutePath}'")
    }

    /**
     * Uses environment variables or properties file to checkout a git repository
     * containing the customization file.
     * @see CustomizationGitProperty
     */
    private fun cloneCustomizationRepository(
        properties: Properties,
        customCheckoutDir: File
    ) {
        val customRepository: String = requireCustomizationProperty(properties, CustomizationGitProperty.CUSTOM_REPOSITORY)
        val customBranch: String = requireCustomizationProperty(properties, CustomizationGitProperty.CUSTOM_BRANCH)
        val gitUser: String = requireCustomizationProperty(properties, CustomizationGitProperty.GIT_USER)
        val gitPassword: String = readCustomizationProperty(properties, CustomizationGitProperty.GIT_PASSWORD).orEmpty()

        println(
            ">> Customization repository specified: checking out branch '$customBranch' of '$customRepository' " +
                "(this can take a while and may hang if git credentials/SSH access are not set up correctly)..."
        )

        if (customCheckoutDir.exists()) {
            customCheckoutDir.deleteRecursively()
        }

        val credentials = Credentials(gitUser, gitPassword)
        cloneWithTimeout(customCheckoutDir, customRepository, customBranch, credentials)

        println(">> Customization repository checked out successfully into '${customCheckoutDir.absolutePath}'")
    }

    /**
     * Runs [Grgit.clone] on a separate, throwaway daemon thread and waits for it for at most
     * [CLONE_TIMEOUT_SECONDS]. Grgit/JGit exposes no timeout option for cloning, so this is enforced
     * manually. A fresh executor is created per call (rather than reused) so that if a clone gets
     * stuck and its thread never unblocks, it does not prevent a subsequent Gradle sync from retrying.
     */
    private fun cloneWithTimeout(
        customCheckoutDir: File,
        customRepository: String,
        customBranch: String,
        credentials: Credentials,
    ) {
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "grgit-customization-clone").apply { isDaemon = true }
        }
        try {
            val future = executor.submit {
                Grgit.clone(mapOf(
                    "dir" to customCheckoutDir,
                    "uri" to customRepository,
                    "refToCheckout" to customBranch,
                    "credentials" to credentials
                ))
            }
            try {
                future.get(CLONE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                future.cancel(true)
                throw IllegalStateException(
                    """
                    |Timed out after ${CLONE_TIMEOUT_SECONDS}s while cloning the customization repository '$customRepository' (branch '$customBranch').
                    |This usually means git could not authenticate non-interactively (e.g. an SSH host-key confirmation or passphrase
                    |prompt with no terminal attached), or the repository/network is unreachable.
                    |Try running 'git clone $customRepository' manually from the same environment Android Studio uses, or remove the
                    |CUSTOM_REPOSITORY/CUSTOM_BRANCH/CUSTOM_FOLDER/CLIENT_FOLDER/GRGIT_USER properties from local.properties to disable customization.
                    """.trimMargin(),
                    e
                )
            } catch (e: ExecutionException) {
                throw IllegalStateException(
                    "Failed to clone customization repository '$customRepository': ${e.cause?.message}",
                    e.cause ?: e
                )
            }
        } finally {
            executor.shutdownNow()
        }
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
         * Mutually exclusive with [CUSTOM_LOCAL_FOLDER]: setting both is an error.
         */
        CUSTOM_REPOSITORY("CUSTOM_REPOSITORY"),

        /**
         * A local folder to use as the customization source, as an alternative to [CUSTOM_REPOSITORY].
         * Instead of checking out a git repository, this folder is copied as-is into the checkout directory.
         * Unlike [CUSTOM_REPOSITORY], this is expected to point directly at the client build folder
         * (i.e. the folder containing `custom-reloaded.json`), so [CUSTOM_FOLDER] and [CLIENT_FOLDER]
         * are not needed (and are ignored) when this is set.
         * Resolved relative to the project's root directory if not an absolute path.
         */
        CUSTOM_LOCAL_FOLDER("CUSTOM_LOCAL_FOLDER"),

        /**
         * The branch of the [CUSTOM_REPOSITORY] to check out.
         */
        CUSTOM_BRANCH("CUSTOM_BRANCH"),

        /**
         * The path to the root of the customization files within the [CUSTOM_REPOSITORY] files.
         * Only used when checking out [CUSTOM_REPOSITORY]; not needed for [CUSTOM_LOCAL_FOLDER].
         */
        CUSTOM_FOLDER("CUSTOM_FOLDER"),

        /**
         * The name of the specific directory within the [CUSTOM_FOLDER] that contains the customization files for the build.
         * Only used when checking out [CUSTOM_REPOSITORY]; not needed for [CUSTOM_LOCAL_FOLDER].
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

    fun isCustomizationEnabled(rootDir: File): Boolean {
        val properties = loadProperties(rootDir)
        return readCustomizationProperty(properties, CustomizationGitProperty.CUSTOM_REPOSITORY) != null ||
            readCustomizationProperty(properties, CustomizationGitProperty.CUSTOM_LOCAL_FOLDER) != null
    }

    private fun readCustomizationProperty(properties: Properties, property: CustomizationGitProperty): String? =
        System.getenv(property.variableName) ?: properties.getProperty(property.variableName)

    private fun requireCustomizationProperty(properties: Properties, property: CustomizationGitProperty): String =
        requireNotNull(readCustomizationProperty(properties, property)) {
            "Missing ${property.variableName} property defined in $GIT_PROPERTIES_FILE_NAME or environment variable"
        }
}
