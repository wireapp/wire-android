package com.wire.android.ui.home.conversations

import androidx.compose.runtime.staticCompositionLocalOf
import com.sebaslogen.resaca.KeyInScopeResolver

val LocalAudioMessageKeyInScopeResolver = staticCompositionLocalOf<KeyInScopeResolver<String>?> { null }
