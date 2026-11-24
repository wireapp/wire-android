/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.gallery

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.navigation.transition.LocalSharedTransitionScope
import com.wire.android.navigation.transition.currentNavAnimatedContentScope

internal fun mediaGalleryImageSharedElementKey(
    messageId: String,
    cellAssetId: String?,
): String = "conversation-image/$messageId/${cellAssetId ?: "main"}"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.mediaGallerySharedElement(
    key: String?,
): Modifier {
    if (key == null) return this

    val sharedTransitionScope = LocalSharedTransitionScope.current
    return with(sharedTransitionScope) {
        sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = currentNavAnimatedContentScope,
        )
    }
}
