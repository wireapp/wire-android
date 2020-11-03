package com.wire.android.core.flags

import com.wire.android.BuildConfig

/**
 * Feature flags states (activated/deactivated) can be used as conditionals.
 *
 * ### Example:
 *
 * ```Flag.Conversations whenActivated { fn } otherwise { fn }```
 */
internal sealed class Flag(enabled: Boolean) : FeatureFlag(enabled) {

    /**
     * Defined Feature Flags.
     * @see "FeatureFlags.kt" file for compile-time feature definition.
     */
    object Search : Flag(BuildConfig.FEATURE_SEARCH)
    object Conversations : Flag(BuildConfig.FEATURE_CONVERSATIONS)
}









