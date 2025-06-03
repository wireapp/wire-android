/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.calling.ongoing.incallreactions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.wire.android.ui.calling.model.InCallReaction
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Keeps state for currently animated reactions
 */
interface InCallReactionsState {
    fun runAnimation(inCallReaction: InCallReaction)
    fun getReactions(): List<AnimatableReaction>
    fun updateHeight(height: Float)
}

object PreviewInCallReactionState : InCallReactionsState {
    override fun runAnimation(inCallReaction: InCallReaction) = Unit
    override fun getReactions(): List<AnimatableReaction> = emptyList()
    override fun updateHeight(height: Float) = Unit
}

@Composable
internal fun rememberInCallReactionsState(): InCallReactionsState {

    val scope = rememberCoroutineScope()

    return remember {
        InCallReactionsStateImpl(
            scope = scope,
            mutableReactions = mutableStateListOf(),
        )
    }
}

private class InCallReactionsStateImpl(
    private val scope: CoroutineScope,
    private val mutableReactions: MutableList<AnimatableReaction>,
) : InCallReactionsState {

    private var currentHeight = 0f

    override fun updateHeight(height: Float) {
        currentHeight = height
        // If there are already reactions on the list after restoring InCallReactionsStateImpl, we need to continue their animations
        continueExistingAnimations()
    }

    /**
     * Used by modifier to draw each animated emoji with current animation state
     */
    override fun getReactions(): List<AnimatableReaction> = mutableReactions.toImmutableList()

    /**
     * Adds new emoji to the list of animated emojis.
     * Runs  animations
     */
    override fun runAnimation(inCallReaction: InCallReaction) {
        scope.launch(Dispatchers.Main) {
            val animatable = AnimatableReaction(
                inCallReaction = inCallReaction,
                horizontalOffset = Random.nextFloat(),
            )

            mutableReactions.add(animatable)
            runAnimations(animatable)
        }
    }

    /**
     * Start transition and fade-out animations, wait for complete and return
     * Removes emoji from the list once animations are complete
     */
    private suspend fun CoroutineScope.runAnimations(reaction: AnimatableReaction) {
        if (currentHeight <= 0f) return // wait for height to be set before running animations, it will re-trigger after setting height
        val remainingOffsetDurationMs = (InCallReactions.animationDurationMs(currentHeight) * (1f - reaction.verticalOffset.value)).toInt()
        val remainingAlphaDurationMs = (InCallReactions.fadeOutAnimationDuarationMs * reaction.alpha.value).toInt()
        val alphaDelayMs = (remainingOffsetDurationMs - remainingAlphaDurationMs).coerceAtLeast(0)
        listOf(
            launch {
                reaction.verticalOffset.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = remainingOffsetDurationMs, easing = LinearEasing),
                )
            },
            launch {
                reaction.alpha.animateTo(
                    targetValue = 0.0f,
                    animationSpec = tween(
                        durationMillis = remainingAlphaDurationMs,
                        delayMillis = alphaDelayMs
                    )
                )
            }
        ).apply {
            joinAll()
            if (this.none { it.isCancelled }) { // remove reaction only if all animations completed successfully
                mutableReactions.remove(reaction)
            }
        }
    }

    private fun continueExistingAnimations() {
        mutableReactions.forEach { reaction ->
            scope.launch(Dispatchers.Main) {
                runAnimations(reaction)
            }
        }
    }
}

data class AnimatableReaction(
    val inCallReaction: InCallReaction,
    val verticalOffset: Animatable<Float, AnimationVector1D> = Animatable(0f),
    val alpha: Animatable<Float, AnimationVector1D> = Animatable(1f),
    val horizontalOffset: Float,
)
