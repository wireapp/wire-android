/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.conversations.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.ui.calling.getOutgoingCallIntent
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.common.HandleActions
import kotlinx.coroutines.flow.Flow

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
