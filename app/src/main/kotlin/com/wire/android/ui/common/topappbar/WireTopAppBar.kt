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
package com.wire.android.ui.common.topappbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wire.android.navigation.style.BackgroundType
import com.wire.android.ui.calling.getIncomingCallIntent
import com.wire.android.ui.calling.getOutgoingCallIntent
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent

@Composable
fun WireTopAppBar(
    commonTopAppBarState: CommonTopAppBarState,
    modifier: Modifier = Modifier,
    backgroundType: BackgroundType = BackgroundType.Default,
    animateContentSize: Boolean = true,
) {
    val context = LocalContext.current
    CommonTopAppBar(
        modifier = modifier,
        commonTopAppBarState = commonTopAppBarState,
        backgroundType = backgroundType,
        animateContentSize = animateContentSize,
        onReturnToCallClick = { establishedCall ->
            getOngoingCallIntent(
                context = context,
                conversationId = establishedCall.conversationId.toString(),
                userId = establishedCall.userId.toString(),
            ).run {
                context.startActivity(this)
            }
        },
        onReturnToIncomingCallClick = {
            getIncomingCallIntent(
                context = context,
                conversationId = it.conversationId.toString(),
                userId = it.userId.toString(),
            ).run {
                context.startActivity(this)
            }
        },
        onReturnToOutgoingCallClick = {
            getOutgoingCallIntent(
                context = context,
                conversationId = it.conversationId.toString(),
                userId = it.userId.toString(),
            ).run {
                context.startActivity(this)
            }
        }
    )
}
