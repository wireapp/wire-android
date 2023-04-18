/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversations.info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_GROUP_DELETED_NAME
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.navigation.getBackNavArg
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.android.util.ui.toUIText
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class ConversationInfoViewModel @Inject constructor(
    private val qualifiedIdMapper: QualifiedIdMapper,
    override val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observerSelfUser: GetSelfUserUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val dispatchers: DispatcherProvider
) : SavedStateViewModel(savedStateHandle) {

    var conversationInfoViewState by mutableStateOf(ConversationInfoViewState())

    val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    private lateinit var selfUserId: UserId

    init {
        viewModelScope.launch {
            selfUserId = observerSelfUser().first().id
        }
    }

    /*
        If this would be collected in the scope of this ViewModel (in `init` for instance) then there would be a race condition.
        [MessageComposerViewModel] handles the navigating back after removing a group and here it would navigate to home if the group
        is removed without back params indicating that the user actually have just done that. The info about the group being removed
        could appear before the back navigation params. That's why it's being observed in the `LaunchedEffect` in the Composable.
    */
    suspend fun observeConversationDetails() {
        observeConversationDetails(conversationId).collect(::handleConversationDetailsResult)
    }

    private suspend fun handleConversationDetailsResult(conversationDetailsResult: ObserveConversationDetailsUseCase.Result) {
        when (conversationDetailsResult) {
            is ObserveConversationDetailsUseCase.Result.Failure -> handleConversationDetailsFailure(
                conversationDetailsResult.storageFailure
            )

            is ObserveConversationDetailsUseCase.Result.Success -> handleConversationDetails(
                conversationDetailsResult.conversationDetails
            )
        }
    }

    /**
     * TODO: This right now handles only the case when a conversation details doesn't exists.
     * Later we'll have to expand the error cases to different behaviors
     */
    private suspend fun handleConversationDetailsFailure(failure: StorageFailure) {
        when (failure) {
            is StorageFailure.DataNotFound -> {
                if (savedStateHandle.getBackNavArg<String>(EXTRA_GROUP_DELETED_NAME) != null) {
                    // do nothing - this group has just been deleted and it's already handled in MessageComposerViewModel
                } else {
                    navigateToHome()
                }
            }
            is StorageFailure.Generic -> appLogger.e("An error occurred when fetching details of the conversation", failure.rootCause)
        }
    }

    private fun handleConversationDetails(conversationDetails: ConversationDetails) {
        val (isConversationUnavailable, isUserBlocked) = when (conversationDetails) {
            is ConversationDetails.OneOne -> conversationDetails.otherUser
                .run { isUnavailableUser to (connectionStatus == ConnectionState.BLOCKED) }
            else -> false to false
        }

        val detailsData = getConversationDetailsData(conversationDetails)
        conversationInfoViewState = conversationInfoViewState.copy(
            conversationName = getConversationName(conversationDetails, isConversationUnavailable),
            conversationAvatar = getConversationAvatar(conversationDetails),
            conversationDetailsData = detailsData,
            hasUserPermissionToEdit = detailsData !is ConversationDetailsData.None,
            conversationType = conversationDetails.conversation.type
        )
    }

    private fun getConversationDetailsData(conversationDetails: ConversationDetails) =
        when (conversationDetails) {
            is ConversationDetails.Group -> ConversationDetailsData.Group(conversationDetails.conversation.id)
            is ConversationDetails.OneOne -> ConversationDetailsData.OneOne(
                otherUserId = conversationDetails.otherUser.id,
                connectionState = conversationDetails.otherUser.connectionStatus,
                isBlocked = conversationDetails.otherUser.connectionStatus == ConnectionState.BLOCKED,
                isDeleted = conversationDetails.otherUser.deleted
            )

            else -> ConversationDetailsData.None
        }

    private fun getConversationAvatar(conversationDetails: ConversationDetails) =
        when (conversationDetails) {
            is ConversationDetails.OneOne ->
                ConversationAvatar.OneOne(
                    conversationDetails.otherUser.previewPicture?.let {
                        ImageAsset.UserAvatarAsset(wireSessionImageLoader, it)
                    },
                    conversationDetails.otherUser.availabilityStatus
                )

            is ConversationDetails.Group -> ConversationAvatar.Group(conversationDetails.conversation.id)
            else -> ConversationAvatar.None
        }

    private fun getConversationName(
        conversationDetails: ConversationDetails,
        isConversationUnavailable: Boolean
    ) = when (conversationDetails) {
        is ConversationDetails.OneOne -> conversationDetails.otherUser.name.orEmpty()
        else -> conversationDetails.conversation.name.orEmpty()
    }.let {
        when {
            it.isNotEmpty() -> it.toUIText()
            it.isEmpty() && isConversationUnavailable -> UIText.StringResource(R.string.username_unavailable_label)
            else -> UIText.StringResource(R.string.member_name_deleted_label)
        }
    }

    fun navigateToDetails() = viewModelScope.launch(dispatchers.default()) {
        when (val data = conversationInfoViewState.conversationDetailsData) {
            is ConversationDetailsData.OneOne -> navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OtherUserProfile.getRouteWithArgs(
                        listOf(data.otherUserId)
                    )
                )
            )

            is ConversationDetailsData.Group -> navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.GroupConversationDetails.getRouteWithArgs(listOf(data.conversationId))
                )
            )

            ConversationDetailsData.None -> { /* do nothing */
            }
        }
    }

    fun navigateToProfile(mentionUserId: String) {
        viewModelScope.launch {
            if (selfUserId.toString() == mentionUserId) {
                navigateToSelfProfile()
            } else {
                val userId = qualifiedIdMapper.fromStringToQualifiedID(mentionUserId)
                when (conversationInfoViewState.conversationDetailsData) {
                    is ConversationDetailsData.Group -> navigateToOtherProfile(userId, conversationId)
                    else -> navigateToOtherProfile(userId)
                }
            }
        }
    }

    private suspend fun navigateToSelfProfile() =
        navigationManager.navigate(NavigationCommand(NavigationItem.SelfUserProfile.getRouteWithArgs()))

    private suspend fun navigateToOtherProfile(id: UserId, conversationId: QualifiedID? = null) =
        navigationManager.navigate(NavigationCommand(NavigationItem.OtherUserProfile.getRouteWithArgs(listOfNotNull(id, conversationId))))

    private suspend fun navigateToHome() =
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.UPDATE_EXISTED))
}
