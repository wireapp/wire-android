/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.navigation.navigation3

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.togetherWith
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.wire.android.navigation.style.TransitionAnimationType

/**
 * Describes how a Navigation 3 entry is presented.
 *
 * The underlying Navigation 3 metadata is deliberately kept private. This gives feature entry
 * providers a stable Wire-owned API while allowing this type to grow with transition and scene
 * metadata without leaking framework-specific keys into every feature.
 */
class WireEntryPresentation private constructor(
    @PublishedApi
    internal val navEntryMetadata: Map<String, Any>,
    internal val transitionProfile: WireEntryTransitionProfile,
    @PublishedApi
    internal val interactionPolicy: WireTransitionInteractionPolicy,
) {

    companion object {
        /**
         * Presents an entry with Wire's standard horizontal transition.
         *
         * This is deliberately an alias of [Slide], rather than Navigation 3's framework default,
         * so entries migrated without an explicit profile retain the current application motion.
         */
        val Default: WireEntryPresentation
            get() = Slide

        /**
         * The new entry slides in from the right; on back it slides out to the right.
         */
        val Slide: WireEntryPresentation = animated(
            profile = WireEntryTransitionProfile.Slide,
            animationType = TransitionAnimationType.SLIDE,
        )

        /**
         * The new entry rises from the bottom; on back it returns towards the bottom.
         */
        val PopUp: WireEntryPresentation = animated(
            profile = WireEntryTransitionProfile.PopUp,
            animationType = TransitionAnimationType.POP_UP,
        )

        /**
         * Disables forward, back and predictive-back content animation for this entry.
         */
        val None: WireEntryPresentation = animated(
            profile = WireEntryTransitionProfile.None,
            animationType = TransitionAnimationType.NONE,
        )

        /**
         * Presents an entry in a platform dialog over the current scene.
         *
         * This is an explicit platform-dialog policy only. Responsive tablet promotion remains a
         * separate scene-selection concern and must not be inferred from this factory.
         */
        fun dialog(
            properties: DialogProperties = DialogProperties(),
        ): WireEntryPresentation = WireEntryPresentation(
            navEntryMetadata = DialogSceneStrategy.dialog(properties),
            transitionProfile = WireEntryTransitionProfile.Dialog,
            interactionPolicy = WireTransitionInteractionPolicy.BlockUntilSettled,
        )

        private fun animated(
            profile: WireEntryTransitionProfile,
            animationType: TransitionAnimationType,
        ): WireEntryPresentation = WireEntryPresentation(
            navEntryMetadata = transitionMetadata(animationType),
            transitionProfile = profile,
            interactionPolicy = WireTransitionInteractionPolicy.BlockUntilSettled,
        )
    }
}

/**
 * Wire-owned transition names used by tests and migration tooling.
 *
 * Feature entry providers choose [WireEntryPresentation] values and never depend on Navigation 3
 * metadata keys or animation scopes directly.
 */
internal enum class WireEntryTransitionProfile {
    Slide,
    PopUp,
    None,
    Dialog,
}

/**
 * Whether an entry accepts pointer input while its containing scene is changing.
 */
@PublishedApi
internal enum class WireTransitionInteractionPolicy {
    AllowDuringTransition,
    BlockUntilSettled;

    fun shouldBlockInput(
        isTransitionRunning: Boolean,
        isEntryResumed: Boolean,
    ): Boolean = this == BlockUntilSettled && isTransitionRunning && !isEntryResumed
}

private fun transitionMetadata(
    animationType: TransitionAnimationType,
): Map<String, Any> =
    NavDisplay.transitionSpec {
        animationType.forwardContentTransform()
    } + NavDisplay.popTransitionSpec {
        animationType.popContentTransform()
    } + NavDisplay.predictivePopTransitionSpec {
        // The same seekable transform keeps regular and gesture-driven back visually coherent.
        // A future RTL-aware profile can use the swipe-edge argument without changing features.
        animationType.popContentTransform()
    }

internal fun TransitionAnimationType.forwardContentTransform(): ContentTransform =
    enterTransition.togetherWith(exitTransition)

internal fun TransitionAnimationType.popContentTransform(): ContentTransform =
    popEnterTransition.togetherWith(popExitTransition)

internal fun <T : Any> wireDefaultTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    TransitionAnimationType.SLIDE.forwardContentTransform()
}

internal fun <T : Any> wireDefaultPopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    TransitionAnimationType.SLIDE.popContentTransform()
}

internal fun <T : Any> wireDefaultPredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform = {
    TransitionAnimationType.SLIDE.popContentTransform()
}
