/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.calling

import android.app.Application
import android.content.Intent
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CallActivityRoutingTest {

    private val mapper = QualifiedIdMapper(null)

    @Test
    fun givenValidStartingIntent_whenParsed_thenAllTypedArgumentsAreReturned() {
        val intent = baseIntent()
            .putExtra(CallActivity.EXTRA_SCREEN_TYPE, StartingCallScreenType.Incoming.name)
            .putExtra(CallActivity.EXTRA_SHOULD_ANSWER_CALL, true)

        val request = intent.toCallActivityRequest(CallActivityDestination.STARTING, mapper)

        requireNotNull(request)
        assertEquals(USER_ID, request.userId.toString())
        assertEquals(CONVERSATION_ID, request.conversationId.toString())
        assertEquals(CallActivityScreen.Starting(StartingCallScreenType.Incoming), request.screen)
        assertEquals(true, request.shouldAnswerCall)
    }

    @Test
    fun givenReplacementIntentWithoutUser_whenParsed_thenItIsRejectedAtomically() {
        val intent = Intent()
            .putExtra(CallActivity.EXTRA_CONVERSATION_ID, CONVERSATION_ID)
            .putExtra(CallActivity.EXTRA_SCREEN_TYPE, StartingCallScreenType.Outgoing.name)

        assertNull(intent.toCallActivityRequest(CallActivityDestination.STARTING, mapper))
    }

    @Test
    fun givenStartingIntentWithoutKnownScreen_whenParsed_thenItIsRejected() {
        val intent = baseIntent().putExtra(CallActivity.EXTRA_SCREEN_TYPE, "unknown")

        assertNull(intent.toCallActivityRequest(CallActivityDestination.STARTING, mapper))
    }

    @Test
    fun givenOngoingIntent_whenParsed_thenItDoesNotRequireStartingScreenType() {
        val request = baseIntent().toCallActivityRequest(CallActivityDestination.ONGOING, mapper)

        requireNotNull(request)
        assertEquals(CallActivityScreen.Ongoing, request.screen)
    }

    private fun baseIntent() = Intent()
        .putExtra(CallActivity.EXTRA_USER_ID, USER_ID)
        .putExtra(CallActivity.EXTRA_CONVERSATION_ID, CONVERSATION_ID)

    private companion object {
        const val USER_ID = "user@wire.test"
        const val CONVERSATION_ID = "conversation@wire.test"
    }
}
