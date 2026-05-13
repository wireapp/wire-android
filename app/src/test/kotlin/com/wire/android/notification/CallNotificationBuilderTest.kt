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

package com.wire.android.notification

import android.app.Application
import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import com.wire.android.R
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CallNotificationBuilderTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val builder = CallNotificationBuilder(context)

    @Test
    fun `given outgoing group call with blank conversation name when building notification then use default group title`() {
        val notification = builder.getOutgoingCallNotification(
            callNotificationData(conversationName = "", conversationType = Conversation.Type.Group.Regular)
        )

        assertEquals(
            context.getString(R.string.notification_outgoing_call_default_group_name),
            notification.extras.getCharSequence(Notification.EXTRA_TITLE)
        )
    }

    @Test
    fun `given ongoing one to one call with blank caller name when building notification then use default caller title`() {
        val notification = builder.getOngoingCallNotification(
            callNotificationData(conversationType = Conversation.Type.OneOnOne, callerName = "")
        )

        assertEquals(
            context.getString(R.string.notification_call_default_caller_name),
            notification.extras.getCharSequence(Notification.EXTRA_TITLE)
        )
    }

    private fun callNotificationData(
        conversationName: String? = "Conversation name",
        conversationType: Conversation.Type = Conversation.Type.Group.Regular,
        callerName: String? = "Caller name",
    ) = CallNotificationData(
        userId = QualifiedID("user", "example.com"),
        userName = "User name",
        conversationId = ConversationId("conversation", "example.com"),
        conversationName = conversationName,
        conversationType = conversationType,
        callerName = callerName,
        callerTeamName = null,
        callStatus = CallStatus.STARTED
    )
}
