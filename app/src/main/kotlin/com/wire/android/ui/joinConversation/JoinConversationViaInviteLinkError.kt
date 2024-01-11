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
package com.wire.android.ui.joinConversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.kalium.logic.feature.conversation.CheckConversationInviteCodeUseCase

@Composable
fun JoinConversationViaInviteLinkError(
    errorState: JoinConversationViaCodeState.Error,
    onCancel: () -> Unit
) {
    when (errorState.error) {
        CheckConversationInviteCodeUseCase.Result.Failure.GuestLinksDisabled ->
            JoinConversationViaDeepLinkErrorDialog(
                stringResource(id = R.string.join_conversation_via_deeplink_error_link_expired),
                onCancel
            )

        CheckConversationInviteCodeUseCase.Result.Failure.AccessDenied ->
            JoinConversationViaDeepLinkErrorDialog(
                stringResource(id = R.string.join_conversation_via_deeplink_error_max_number_of_participent),
                onCancel
            )

        CheckConversationInviteCodeUseCase.Result.Failure.ConversationNotFound,
        CheckConversationInviteCodeUseCase.Result.Failure.InvalidCodeOrKey,
        CheckConversationInviteCodeUseCase.Result.Failure.RequestingUserIsNotATeamMember,
        is CheckConversationInviteCodeUseCase.Result.Failure.Generic ->
            JoinConversationViaDeepLinkErrorDialog(
                stringResource(id = R.string.join_conversation_via_deeplink_error_general),
                onCancel
            )
    }
}
