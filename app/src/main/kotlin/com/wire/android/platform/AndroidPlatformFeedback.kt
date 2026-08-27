/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.platform

import com.wire.android.R
import com.wire.android.media.PingRinger
import com.wire.media.player.PlatformFeedback
import com.wire.media.player.PlatformFeedbackResult
import com.wire.media.player.PlatformFeedbackType
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<PlatformFeedback>())
class AndroidPlatformFeedback(
    private val pingRinger: PingRinger,
) : PlatformFeedback {
    override fun perform(type: PlatformFeedbackType): PlatformFeedbackResult = when (type) {
        PlatformFeedbackType.OUTGOING_PING -> {
            pingRinger.ping(R.raw.ping_from_me, isReceivingPing = false)
            PlatformFeedbackResult.Performed
        }
    }
}
