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
