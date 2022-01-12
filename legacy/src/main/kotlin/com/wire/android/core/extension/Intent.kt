package com.wire.android.core.extension

import android.content.Intent

/**
 * Finishes all activities created before this Intent.
 */
fun Intent.clearStack() = addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
