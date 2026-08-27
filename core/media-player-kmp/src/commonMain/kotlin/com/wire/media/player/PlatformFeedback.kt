/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.media.player

enum class PlatformFeedbackType {
    OUTGOING_PING,
}

sealed interface PlatformFeedbackResult {
    data object Performed : PlatformFeedbackResult
    data object Unsupported : PlatformFeedbackResult
    data class Failure(val reason: String? = null) : PlatformFeedbackResult
}

fun interface PlatformFeedback {
    fun perform(type: PlatformFeedbackType): PlatformFeedbackResult
}

object UnsupportedPlatformFeedback : PlatformFeedback {
    override fun perform(type: PlatformFeedbackType): PlatformFeedbackResult = PlatformFeedbackResult.Unsupported
}
