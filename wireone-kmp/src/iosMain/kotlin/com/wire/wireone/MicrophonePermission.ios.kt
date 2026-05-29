@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.wire.wireone

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioSession
import kotlin.coroutines.resume

internal actual suspend fun requestMicrophonePermissionIfNeeded(): Boolean {
    val audioSession = AVAudioSession.sharedInstance()
    return suspendCancellableCoroutine { continuation ->
        audioSession.requestRecordPermission { granted ->
            continuation.resume(granted)
        }
    }
}

