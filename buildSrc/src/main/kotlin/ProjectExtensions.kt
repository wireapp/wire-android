import org.gradle.api.Project
import java.util.Properties

/**
 * Convenience method to obtain a property from `$projectRoot/local.properties` file
 * without passing the project param
 */
fun <T> Project.getLocalProperty(propertyName: String, defaultValue: T): T {
    return getLocalProperty(propertyName, defaultValue, this)
}

/**
 * Util to obtain property declared on `$projectRoot/local.properties` file or default
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> getLocalProperty(propertyName: String, defaultValue: T, project: Project): T {
    val localProperties = Properties().apply {
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            load(localPropertiesFile.inputStream())
        }
    }

    val localValue = localProperties.getOrDefault(propertyName, defaultValue) as? T ?: defaultValue
    println("> Reading local prop '$propertyName' with value: $localValue")
    return localValue
}
