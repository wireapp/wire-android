/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.util.debug

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Keys of the experimental features that can be toggled from the debug screen.
 *
 * They must match the `feature_*` entries of `default.json`, which the build exposes through
 * [BuildConfig.EXPERIMENTAL_FEATURES].
 */
object ExperimentalFeatureKeys {
    const val OFFLINE_FILES = "feature_offline_files_enabled"
    const val IN_APP_IMAGE_VIEWER = "feature_in_app_image_viewer_enabled"
}

/** Kept in sync with `EXPERIMENTAL_FEATURE_PREFIX` in `buildSrc`. */
private const val EXPERIMENTAL_FEATURE_PREFIX = "feature_"

/**
 * A single `feature_*` flag, together with the value it was compiled with and the local override,
 * if the user set one from the debug screen.
 */
data class ExperimentalFeature(
    val key: String,
    val compiledValue: Boolean,
    val override: Boolean?,
) {
    val isEnabled: Boolean get() = override ?: compiledValue
    val isOverridden: Boolean get() = override != null && override != compiledValue

    /** `feature_offline_files_enabled` -> `Offline files`. */
    val name: String
        get() = key.removePrefix(EXPERIMENTAL_FEATURE_PREFIX)
            .removeSuffix("_enabled")
            .replace('_', ' ')
            .replaceFirstChar { it.uppercase() }
}

/**
 * Local overrides for the compile time `feature_*` flags, so that they can be experimented with
 * without producing a new build.
 *
 * The flags are read once while the app is starting up, so a change only takes effect after the app
 * is restarted, see [isRestartRequired].
 *
 * Overrides are ignored outside of private builds: public builds always use the compiled values.
 */
@SingleIn(AppScope::class)
class ExperimentalFeaturesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val compiledValues: Map<String, Boolean> = BuildConfig.EXPERIMENTAL_FEATURES
    private val overridesAllowed: Boolean = BuildConfig.PRIVATE_BUILD

    private val preferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Values the running process was started with. Anything diverging from them needs a restart to
     * be picked up by the rest of the app.
     */
    private val valuesAtStartup: Map<String, Boolean> = compiledValues.keys.associateWith(::isEnabled)

    fun isEnabled(key: String): Boolean {
        val compiledValue = compiledValues[key]
        if (compiledValue == null) {
            appLogger.e("Unknown experimental feature '$key', it is missing from default.json")
            return false
        }
        return if (overridesAllowed) preferences.getBoolean(key, compiledValue) else compiledValue
    }

    fun all(): List<ExperimentalFeature> = compiledValues.entries
        .map { (key, compiledValue) ->
            ExperimentalFeature(
                key = key,
                compiledValue = compiledValue,
                override = if (overridesAllowed && preferences.contains(key)) preferences.getBoolean(key, compiledValue) else null,
            )
        }
        .sortedBy(ExperimentalFeature::name)

    fun setEnabled(key: String, enabled: Boolean) {
        preferences.edit().putBoolean(key, enabled).apply()
    }

    fun resetAll() {
        preferences.edit().clear().apply()
    }

    /** Whether at least one flag currently differs from the value the process was started with. */
    fun isRestartRequired(): Boolean = valuesAtStartup.any { (key, valueAtStartup) -> isEnabled(key) != valueAtStartup }

    companion object {
        @VisibleForTesting
        const val PREFERENCES_NAME = "experimental_features"
    }
}