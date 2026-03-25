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

import io.kayan.ConfigSchema
import io.kayan.ConfigValue
import io.kayan.DefaultConfigResolver
import java.io.File

class ConfigurationFileImporter {

    private val resolver = DefaultConfigResolver()
    private val schema = ConfigSchema(FeatureConfigs.entries.map { it.toConfigDefinition() })

    fun loadConfigsFromFile(file: File): NormalizedFlavorSettings {
        return loadConfigsFromFiles(baseConfigFile = file)
    }

    fun loadConfigsFromFiles(
        baseConfigFile: File,
        customConfigFile: File? = null
    ): NormalizedFlavorSettings {
        val resolvedConfig = resolver.resolve(
            defaultConfigJson = baseConfigFile.readText(),
            schema = schema,
            customConfigJson = customConfigFile?.readText(),
            defaultConfigSourceName = baseConfigFile.absolutePath,
            customConfigSourceName = customConfigFile?.absolutePath ?: "custom config"
        )

        return NormalizedFlavorSettings(
            resolvedConfig.flavors.mapValues { (_, flavorConfig) ->
                flavorConfig.values.mapKeys { (definition, _) -> definition.jsonKey }
                    .mapValues { (_, value) -> value.toRawValue() }
            }
        )
    }

    private fun ConfigValue?.toRawValue(): Any? = when (this) {
        null -> null
        is ConfigValue.StringValue -> value
        is ConfigValue.BooleanValue -> value
        is ConfigValue.IntValue -> value
        is ConfigValue.LongValue -> value
        is ConfigValue.DoubleValue -> value
        is ConfigValue.StringMapValue -> value
        is ConfigValue.StringListMapValue -> value
        is ConfigValue.StringListValue -> value
        is ConfigValue.EnumValue -> value
        is ConfigValue.NullValue -> null
    }

}
