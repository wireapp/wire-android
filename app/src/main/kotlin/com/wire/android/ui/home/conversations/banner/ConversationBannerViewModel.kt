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

package com.wire.android.ui.home.conversations.banner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.banner.usecase.ObserveConversationMembersByTypesUseCase
import com.wire.android.ui.navArgs
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.conversation.NotifyConversationIsOpenUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConversationBannerViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val observeConversationMembersByTypes: ObserveConversationMembersByTypesUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val notifyConversationIsOpen: NotifyConversationIsOpenUseCase,
) : ViewModel() {

    var bannerState by mutableStateOf<UIText?>(null)

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = conversationNavArgs.conversationId

    init {
        viewModelScope.launch {
            observeConversationDetails(conversationId)
                .filter {
                    when (it) {
                        is ObserveConversationDetailsUseCase.Result.Failure -> false
                        is ObserveConversationDetailsUseCase.Result.Success -> it.conversationDetails is ConversationDetails.Group
                    }
                }
                .flatMapLatest { observeConversationMembersByTypes(conversationId) }
                .collect(::handleConversationMemberTypes)
        }
        viewModelScope.launch {
            notifyConversationIsOpen(conversationId)
        }
    }

    @Suppress("ComplexMethod")
    private fun handleConversationMemberTypes(userTypes: Set<UserType>) {
        val containsGuests = userTypes.contains(UserType.GUEST)
        val containsFederated = userTypes.contains(UserType.FEDERATED)
        val containsExternal = userTypes.contains(UserType.EXTERNAL)
        val containsService = userTypes.contains(UserType.SERVICE)

        bannerState = when {
            (containsFederated && containsExternal && containsGuests && containsService)
            -> UIText.StringResource(R.string.conversation_banner_federated_externals_guests_services_present)
            (containsFederated && containsExternal && containsGuests)
            -> UIText.StringResource(R.string.conversation_banner_federated_externals_guests_present)
            (containsFederated && containsExternal && containsService)
            -> UIText.StringResource(R.string.conversation_banner_federated_externals_services_present)
            (containsFederated && containsGuests && containsService)
            -> UIText.StringResource(R.string.conversation_banner_federated_guests_services_present)
            (containsExternal && containsGuests && containsService)
            -> UIText.StringResource(R.string.conversation_banner_externals_guests_services_present)
            (containsFederated && containsService) -> UIText.StringResource(R.string.conversation_banner_federated_services_present)
            (containsFederated && containsGuests) -> UIText.StringResource(R.string.conversation_banner_federated_guests_present)
            (containsFederated && containsExternal) -> UIText.StringResource(R.string.conversation_banner_federated_externals_present)
            (containsExternal && containsService) -> UIText.StringResource(R.string.conversation_banner_externals_services_present)
            (containsExternal && containsGuests) -> UIText.StringResource(R.string.conversation_banner_externals_guests_present)
            (containsGuests && containsService) -> UIText.StringResource(R.string.conversation_banner_guests_services_present)
            (containsFederated) -> UIText.StringResource(R.string.conversation_banner_federated_present)
            (containsGuests) -> UIText.StringResource(R.string.conversation_banner_guests_present)
            (containsExternal) -> UIText.StringResource(R.string.conversation_banner_externals_present)
            (containsService) -> UIText.StringResource(R.string.conversation_banner_services_active)
            else -> null
        }
    }
}
