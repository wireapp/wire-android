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

package com.wire.android.ui.home.conversations.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.ui.calling.getOutgoingCallIntent
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.common.HandleActions
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.flow.Flow

sealed interface JoinOrStartCallViewActions {
    data class JoinedCall(val conversationId: ConversationId, val userId: UserId) : JoinOrStartCallViewActions
    data class InitiatedCall(val conversationId: ConversationId, val userId: UserId) : JoinOrStartCallViewActions
}

@Composable
fun Flow<JoinOrStartCallViewActions>.HandleActions() {
    val context = LocalContext.current
    HandleActions(this) { action ->
        when (action) {
            is JoinOrStartCallViewActions.InitiatedCall -> {
                context.startActivity(getOutgoingCallIntent(context, action.conversationId.toString(), action.userId.toString()))
                AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.CallInitiated)
            }

            is JoinOrStartCallViewActions.JoinedCall -> {
                context.startActivity(getOngoingCallIntent(context, action.conversationId.toString(), action.userId.toString()))
                AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.CallJoined)
            }
        }
    }
}
