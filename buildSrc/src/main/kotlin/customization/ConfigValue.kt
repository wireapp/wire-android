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

/**
 * Represents a configuration value that can be placed either in BuildConfig or AndroidManifest.
 * This allows fine-grained control over where configuration values are stored.
 */
sealed class ConfigValue {
    /**
     * A value that should be placed in BuildConfig as a static field.
     * These values are compiled into the APK and accessible via BuildConfig class.
     */
    data class BuildConfigValue(val value: Any?) : ConfigValue()
    
    /**
     * A value that should be placed in AndroidManifest.xml as a placeholder.
     * These values are resolved at build time and embedded in the manifest.
     * Useful for sensitive data that shouldn't be easily accessible via reverse engineering.
     */
    data class ManifestPlaceholderValue(val value: Any?) : ConfigValue()
}

/**
 * Extension function to get the actual value regardless of the ConfigValue type.
 */
fun ConfigValue.getRawValue(): Any? = when (this) {
    is ConfigValue.BuildConfigValue -> value
    is ConfigValue.ManifestPlaceholderValue -> value
}