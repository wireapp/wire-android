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

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireEntryPresentationTest {

    @Test
    fun givenDefaultPresentation_whenResolved_thenItUsesTheCurrentSlideProfile() {
        assertEquals(WireEntryPresentation.Slide, WireEntryPresentation.Default)
        assertEquals(
            WireEntryTransitionProfile.Slide,
            WireEntryPresentation.Default.transitionProfile,
        )
        assertTransitionMetadataIsComplete(WireEntryPresentation.Default)
    }

    @Test
    fun givenEachAnimatedPresentation_whenMetadataIsCreated_thenAllTransitionDirectionsAreDefined() {
        assertEquals(
            WireEntryTransitionProfile.Slide,
            WireEntryPresentation.Slide.transitionProfile,
        )
        assertEquals(
            WireEntryTransitionProfile.PopUp,
            WireEntryPresentation.PopUp.transitionProfile,
        )
        assertEquals(
            WireEntryTransitionProfile.None,
            WireEntryPresentation.None.transitionProfile,
        )

        assertTransitionMetadataIsComplete(WireEntryPresentation.Slide)
        assertTransitionMetadataIsComplete(WireEntryPresentation.PopUp)
        assertTransitionMetadataIsComplete(WireEntryPresentation.None)
    }

    @Test
    fun givenDialogPresentation_whenMetadataIsCreated_thenDialogPropertiesArePreserved() {
        val properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        )

        val metadata = WireEntryPresentation.dialog(properties).navEntryMetadata

        assertEquals(DialogSceneStrategy.dialog(properties), metadata)
        assertEquals(
            WireEntryTransitionProfile.Dialog,
            WireEntryPresentation.dialog(properties).transitionProfile,
        )
    }

    @Test
    fun givenBlockUntilSettledPolicy_whenTransitionRunsBeforeResume_thenInputIsBlocked() {
        val policy = WireTransitionInteractionPolicy.BlockUntilSettled

        assertTrue(policy.shouldBlockInput(isTransitionRunning = true, isEntryResumed = false))
    }

    @Test
    fun givenLifecycleLagsAfterTransition_whenEvaluatingInput_thenInputIsAllowed() {
        assertFalse(
            WireTransitionInteractionPolicy.BlockUntilSettled.shouldBlockInput(
                isTransitionRunning = false,
                isEntryResumed = false,
            )
        )
    }

    @Test
    fun givenAnimationSignalIsStaleAfterResume_whenEvaluatingInput_thenInputIsAllowed() {
        assertFalse(
            WireTransitionInteractionPolicy.BlockUntilSettled.shouldBlockInput(
                isTransitionRunning = true,
                isEntryResumed = true,
            )
        )
    }

    @Test
    fun givenAllowDuringTransitionPolicy_whenTransitionRuns_thenInputRemainsEnabled() {
        assertFalse(
            WireTransitionInteractionPolicy.AllowDuringTransition.shouldBlockInput(
                isTransitionRunning = true,
                isEntryResumed = false,
            )
        )
    }

    private fun assertTransitionMetadataIsComplete(presentation: WireEntryPresentation) {
        val transitionKey = NavDisplay.transitionSpec { null }.keys.single()
        val popTransitionKey = NavDisplay.popTransitionSpec { null }.keys.single()
        val predictivePopTransitionKey =
            NavDisplay.predictivePopTransitionSpec { null }.keys.single()

        assertTrue(transitionKey in presentation.navEntryMetadata)
        assertTrue(popTransitionKey in presentation.navEntryMetadata)
        assertTrue(predictivePopTransitionKey in presentation.navEntryMetadata)
    }
}
