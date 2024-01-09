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

class ConfigurationFileImporter {

    private val jsonReader = JsonSlurper()

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

    internal companion object {
        const val KEY_FLAVORS = "flavors"
    }
}
