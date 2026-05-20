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
package com.wire.android.ui.home.conversations.details.editguestaccess

import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.SyncConversationCodeUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.CanCreatePasswordProtectedLinksUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.ObserveGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.ObserveGuestRoomLinkFeatureFlagUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class EditGuestAccessViewModelFactory(
    private val dispatcher: DispatcherProvider,
    private val updateConversationAccessRole: UpdateConversationAccessRoleUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val generateGuestRoomLink: GenerateGuestRoomLinkUseCase,
    private val revokeGuestRoomLink: RevokeGuestRoomLinkUseCase,
    private val observeGuestRoomLink: ObserveGuestRoomLinkUseCase,
    private val observeGuestRoomLinkFeatureFlag: ObserveGuestRoomLinkFeatureFlagUseCase,
    private val canCreatePasswordProtectedLinks: CanCreatePasswordProtectedLinksUseCase,
    private val syncConversationCode: SyncConversationCodeUseCase,
    private val getDefaultProtocol: GetDefaultProtocolUseCase,
    private val selfUser: ObserveSelfUserUseCase,
) {
    fun create(args: EditGuestAccessNavArgs): EditGuestAccessViewModel = EditGuestAccessViewModel(
        editGuestAccessNavArgs = args,
        dispatcher = dispatcher,
        updateConversationAccessRole = updateConversationAccessRole,
        observeConversationDetails = observeConversationDetails,
        observeConversationMembers = observeConversationMembers,
        generateGuestRoomLink = generateGuestRoomLink,
        revokeGuestRoomLink = revokeGuestRoomLink,
        observeGuestRoomLink = observeGuestRoomLink,
        observeGuestRoomLinkFeatureFlag = observeGuestRoomLinkFeatureFlag,
        canCreatePasswordProtectedLinks = canCreatePasswordProtectedLinks,
        syncConversationCode = syncConversationCode,
        getDefaultProtocol = getDefaultProtocol,
        selfUser = selfUser,
    )
}
