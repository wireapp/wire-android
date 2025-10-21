
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.ByteArrayOutputStream
import java.util.Properties

// Apply your test library plugin
plugins {
    id(libs.plugins.wire.android.test.library.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

// Map to store environment variables (secrets)
val env = Properties()

// File where secrets will be saved/generated
val secretsJson =  rootProject.file("secrets.json")

// Function to sanitize keys by replacing spaces and dashes with underscores, and making uppercase
fun sanitize(text: String): String {
    return text.replace("[.\\-\\s]+".toRegex(), "_").uppercase()
}

// Function to escape special characters for BuildConfig string fields
fun escapeForBuildConfig(value: String): String {
    return value
        .replace("\\", "\\\\")  // Escape backslash
        .replace("\"", "\\\"")  // Escape quotes
        .replace("\n", "\\n")   // Escape new lines
        .replace("\r", "")      // Remove carriage returns (optional)
}

// If secrets.json exists, parse and load values into env map
if (secretsJson.exists()) {
    // Parse secrets JSON as a map of item title -> item details
    val parsed = JsonSlurper().parse(secretsJson) as Map<String, Map<String, Any>>

    parsed.forEach { (title, item) ->
        val sectionName = sanitize(title)  // Sanitize the section/item title

        // Get the fields as a map of label -> field details
        val fields = item["fields"] as? Map<String, Map<String, Any>> ?: emptyMap()

        // For each field, create env variable with sanitized key and store value
        for ((label, field) in fields) {
            val key = "${sectionName}_${sanitize(label)}"
            val value = field["value"]?.toString() ?: ""
            env[key] = value
        }
    }
}

android {
    namespace = "com.wire.android.testSupport"

    defaultConfig {
        // Inject environment variables as BuildConfig fields
        env.forEach { (key, value) ->
            buildConfigField("String", key.toString(), "\"${escapeForBuildConfig(value.toString())}\"")
        }
    }

    buildFeatures {
        buildConfig = true  // Enable generation of BuildConfig class
    }
}

// Just in case, enforce BuildConfig enabled flag (redundant but explicit)
android.buildFeatures.buildConfig = true

dependencies {
    // Android test dependencies
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.uiAutomator)
    implementation(libs.datafaker)
}

// Register a custom Gradle task 'fetchSecrets' to fetch secrets from 1Password CLI and generate secrets.json
tasks.register("fetchSecrets") {
    // Only run if secrets.json doesn't exist or needs updating
  outputs.file(rootProject.file("secrets.json"))

    doLast {
      val secretsFile = rootProject.file("secrets.json")
        if (!secretsFile.exists()) {
            val vaultName = "Test Automation"

            // Helper function to execute shell commands and capture output
            fun runCommand(command: List<String>): String {
                val output = ByteArrayOutputStream()
                project.exec {
                    commandLine = command
                    standardOutput = output
                }
                return output.toString().trim()
            }

            // 1. List all items in the vault, output in JSON format
            val listOutput = runCommand(listOf("op", "item", "list", "--vault", vaultName, "--format", "json"))

            // Parse the list output into a List of maps
            val items = JsonSlurper().parseText(listOutput) as List<Map<String, Any>>

            // Mutable map to combine all fetched secrets into one JSON object
            val combinedSecrets = mutableMapOf<String, Any>()

            items.forEach { item ->
                val itemId = item["id"] as? String ?: return@forEach
                val itemTitle = item["title"] as? String ?: return@forEach

                // 2. Fetch each secret item's full details
                val itemOutput = runCommand(listOf("op", "item", "get", itemId, "--format", "json"))
                val itemData = JsonSlurper().parseText(itemOutput) as Map<String, Any>

                // 3. Convert fields from List to Map where label is the key (simplify structure)
                val rawFields = itemData["fields"] as? List<Map<String, Any>> ?: emptyList()
                val fieldsMap = mutableMapOf<String, Map<String, Any?>>()
                rawFields.forEachIndexed { index, field ->
                    val label = field["label"] as? String ?: return@forEachIndexed
                    // If label already exists, append index to make it unique
                    val uniqueLabel = if (fieldsMap.containsKey(label)) "${label}_$index" else label

                    fieldsMap[uniqueLabel] = mapOf(
                        "type" to field["type"],
                        "value" to field["value"]
                    )
                }

                // Replace original fields list with simplified map
                val simplifiedItemData = itemData.toMutableMap()
                simplifiedItemData["fields"] = fieldsMap

                combinedSecrets[itemTitle] = simplifiedItemData
            }

            // Ensure directory exists and write combined secrets to secrets.json with pretty formatting
            secretsFile.parentFile.mkdirs()
            secretsFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(combinedSecrets)))

            println("✅ Simplified secrets saved to ${secretsFile.path}")
        } else {
            println("ℹ️ secrets.json already exists - skipping fetch")
        }
    }

}
// workaround for now, we should configure the action https://github.com/1Password/install-cli-action when running tests on CI
val isGitHubActions = System.getenv("GITHUB_ACTIONS") == "true"
if (!isGitHubActions) {
    // Make sure fetchSecrets runs automatically before building
    tasks.named("preBuild") {
        dependsOn("fetchSecrets")
    }
}
