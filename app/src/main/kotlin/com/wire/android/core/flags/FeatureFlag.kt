package com.wire.android.core.flags

internal open class FeatureFlag(private val enabled: Boolean) {

    infix fun whenActivated(fnFeatureEnabled: () -> Unit): Condition {
        if (enabled) fnFeatureEnabled.invoke(); return Condition(enabled)
    }

    inner class Condition(private val expression: Boolean) {
        infix fun otherwise(otherwise: () -> Unit) {
            if (!expression) otherwise.invoke()
        }
    }
}
