package customization

import java.io.File

data class BuildTimeConfiguration(
    val flavorSettings: NormalizedFlavorSettings,
    val customResourceOverrideDirectory: File?
)

data class NormalizedFlavorSettings(
    val flavorMap: Map<String, Map<String, Any?>>
)

/**
 * Creates a copy of [this], replacing all its values
 * with the values specified in [overrides].
 *
 * Similar to [Map.plus], but this provides a more granular
 * exception handling and customisation of the override condition, if desired.
 */
internal fun Map<String, Any?>.overwritingWith(
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
