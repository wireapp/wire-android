/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.media.audiomessage

import com.wire.android.services.ServicesManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

interface ConversationAudioPlatformController {
    fun setFocusListener(onPause: () -> Unit, onResume: () -> Unit)
    fun requestFocus(): Boolean
    fun abandonFocus()
    fun startPlaybackService()
    fun stopPlaybackService()
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ConversationAudioPlatformController>())
class AndroidConversationAudioPlatformController(
    private val servicesManager: Lazy<ServicesManager>,
    private val audioFocusHelper: AudioFocusHelper,
) : ConversationAudioPlatformController {
    override fun setFocusListener(onPause: () -> Unit, onResume: () -> Unit) {
        audioFocusHelper.setListener(onPause, onResume)
    }

    override fun requestFocus(): Boolean = audioFocusHelper.request()

    override fun abandonFocus() = audioFocusHelper.abandon()

    override fun startPlaybackService() = servicesManager.value.startPlayingAudioMessageService()

    override fun stopPlaybackService() = servicesManager.value.stopPlayingAudioMessageService()
}
