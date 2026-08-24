/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.devices

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

private val deviceFadeAnimationSpec: FiniteAnimationSpec<Float> = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = Spring.DefaultDisplacementThreshold,
)
private val deviceSlideAnimationSpec: FiniteAnimationSpec<IntOffset> = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.Companion.VisibilityThreshold,
)

/** Loading-to-code transition retained from the former host-owned presentation. */
@Suppress("MagicNumber")
internal fun deviceAuthenticationSlideTransition(): ContentTransform =
    (slideInHorizontally(
        animationSpec = deviceSlideAnimationSpec,
        initialOffsetX = { fullWidth -> fullWidth / 3 },
    ) + fadeIn(animationSpec = deviceFadeAnimationSpec))
        .togetherWith(fadeOut(animationSpec = deviceFadeAnimationSpec))
