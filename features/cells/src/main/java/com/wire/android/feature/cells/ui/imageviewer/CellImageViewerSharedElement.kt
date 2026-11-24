/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.cells.ui.imageviewer

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.navigation.transition.LocalSharedTransitionScope
import com.wire.android.navigation.transition.currentNavAnimatedContentScope

internal fun cellImageViewerSharedElementKey(fileUuid: String): String =
    "cell-image/$fileUuid"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.cellImageViewerSharedElement(fileUuid: String?): Modifier {
    if (fileUuid == null) return this

    val sharedTransitionScope = LocalSharedTransitionScope.current
    return with(sharedTransitionScope) {
        sharedElement(
            sharedContentState = rememberSharedContentState(
                cellImageViewerSharedElementKey(fileUuid)
            ),
            animatedVisibilityScope = currentNavAnimatedContentScope,
        )
    }
}
