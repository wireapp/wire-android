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

package com.wire.android.ui.home.conversationslist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.wire.android.BuildConfig
import com.wire.android.di.CurrentAccount
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.mapper.toConversationItem
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.model.SnackBarMessage
import com.wire.android.ui.common.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.android.ui.home.HomeSnackBarMessage
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.android.ui.home.conversationslist.common.previewConversationItemsFlow
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.ConversationSection
import com.wire.android.ui.home.conversationslist.model.ConversationItemType
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationFilter
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsWithEventsUseCase
import com.wire.kalium.logic.feature.conversation.RefreshConversationsWithoutMetadataUseCase
import com.wire.kalium.logic.feature.legalhold.LegalHoldStateForSelfUser
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
interface ConversationListViewModel {
    val infoMessage: SharedFlow<SnackBarMessage> get() = MutableSharedFlow()
    val requestInProgress: Boolean get() = false
    val conversationListState: ConversationListState get() = ConversationListState.Paginated(emptyFlow())
    suspend fun refreshMissingMetadata() {}
    fun searchQueryChanged(searchQuery: String) {}
    fun playPauseCurrentAudio() {}
    fun stopCurrentAudio() {}
}

@Suppress("TooManyFunctions")
class ConversationListViewModelPreview(
    sectionsWithConversations: Flow<PagingData<ConversationItemType>> = previewConversationItemsFlow(),
) : ConversationListViewModel {
    override val conversationListState = ConversationListState.Paginated(sectionsWithConversations)
}

@Suppress("MagicNumber", "TooManyFunctions", "LongParameterList")
@HiltViewModel(assistedFactory = ConversationListViewModelImpl.Factory::class)
class ConversationListViewModelImpl @AssistedInject constructor(
    @Assisted val conversationsSource: ConversationsSource,
    @Assisted private val usePagination: Boolean = BuildConfig.PAGINATED_CONVERSATION_LIST_ENABLED,
    private val dispatcher: DispatcherProvider,
    private val getConversationsPaginated: GetConversationsFromSearchUseCase,
    private val observeConversationListDetailsWithEvents: ObserveConversationListDetailsWithEventsUseCase,
    private val refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
    private val refreshConversationsWithoutMetadata: RefreshConversationsWithoutMetadataUseCase,
    private val observeLegalHoldStateForSelfUser: ObserveLegalHoldStateForSelfUserUseCase,
    private val audioMessagePlayer: ConversationAudioMessagePlayer,
    @CurrentAccount val currentAccount: UserId,
    private val userTypeMapper: UserTypeMapper,
    private val getSelfUser: GetSelfUserUseCase,
) : ConversationListViewModel, ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            conversationsSource: ConversationsSource,
            usePagination: Boolean = BuildConfig.PAGINATED_CONVERSATION_LIST_ENABLED,
        ): ConversationListViewModelImpl
    }

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    override val infoMessage = _infoMessage.asSharedFlow()

    private var _requestInProgress: Boolean by mutableStateOf(false)
    override val requestInProgress: Boolean get() = _requestInProgress

    private val searchQueryFlow: MutableStateFlow<String> = MutableStateFlow("")
    private val isSelfUserUnderLegalHoldFlow = MutableSharedFlow<Boolean>(replay = 1)

    private val containsNewActivitiesSection = when (conversationsSource) {
        ConversationsSource.MAIN,
        ConversationsSource.FAVORITES,
        is ConversationsSource.FOLDER,
        ConversationsSource.GROUPS,
        ConversationsSource.CHANNELS,
        ConversationsSource.ONE_ON_ONE -> true

        ConversationsSource.ARCHIVE -> false
    }

    private val conversationsPaginatedFlow: Flow<PagingData<ConversationItemType>> = searchQueryFlow
        .debounce { if (it.isEmpty()) 0L else DEFAULT_SEARCH_QUERY_DEBOUNCE }
        .onStart { emit("") }
        .combine(isSelfUserUnderLegalHoldFlow, ::Pair)
        .distinctUntilChanged()
        .combine(audioMessagePlayer.playingAudioMessageFlow) { (searchQuery, isSelfUserUnderLegalHold), playingAudioMessage ->
            Triple(searchQuery, isSelfUserUnderLegalHold, playingAudioMessage)
        }
        .flatMapLatest { (searchQuery, isSelfUserUnderLegalHold, playingAudioMessage) ->
            getConversationsPaginated(
                searchQuery = searchQuery,
                fromArchive = conversationsSource == ConversationsSource.ARCHIVE,
                conversationFilter = conversationsSource.toFilter(),
                onlyInteractionEnabled = false,
                newActivitiesOnTop = containsNewActivitiesSection,
                playingAudioMessage = playingAudioMessage,
                useStrictMlsFilter = BuildConfig.USE_STRICT_MLS_FILTER,
            ).map { pagingData ->
                pagingData
                    .map { it.hideIndicatorForSelfUserUnderLegalHold(isSelfUserUnderLegalHold) }
                    .insertSeparators { before, after ->
                        when {
                            // do not add separators if the list shouldn't show conversations grouped into different folders
                            !containsNewActivitiesSection -> null

                            before == null && after != null && after.hasNewActivitiesToShow ->
                                // list starts with items with "new activities"
                                ConversationSection.Predefined.NewActivities

                            before == null && after != null && !after.hasNewActivitiesToShow ->
                                // list doesn't contain any items with "new activities"
                                ConversationSection.Predefined.Conversations

                            before != null && before.hasNewActivitiesToShow && after != null && !after.hasNewActivitiesToShow ->
                                // end of "new activities" section and beginning of "conversations" section
                                ConversationSection.Predefined.Conversations

                            else -> null
                        }
                    }
            }
        }
        .flowOn(dispatcher.io())
        .cachedIn(viewModelScope)

    override var conversationListState by mutableStateOf(
        when (usePagination) {
            true -> ConversationListState.Paginated(conversations = conversationsPaginatedFlow, domain = currentAccount.domain)
            false -> ConversationListState.NotPaginated()
        }
    )
        private set

    init {
        observeSelfUserLegalHoldState()
        if (!usePagination) {
            observeNonPaginatedSearchConversationList()
        }
    }

    private fun observeSelfUserLegalHoldState() {
        viewModelScope.launch {
            observeLegalHoldStateForSelfUser()
                .map { it is LegalHoldStateForSelfUser.Enabled }
                .flowOn(dispatcher.io())
                .collect { isSelfUserUnderLegalHoldFlow.emit(it) }
        }
    }

    private fun observeNonPaginatedSearchConversationList() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce { if (it.isEmpty()) 0L else DEFAULT_SEARCH_QUERY_DEBOUNCE }
                .onStart { emit("") }
                .distinctUntilChanged()
                .flatMapLatest { searchQuery: String ->
                    combine(
                        observeConversationListDetailsWithEvents(
                            fromArchive = conversationsSource == ConversationsSource.ARCHIVE,
                            conversationFilter = conversationsSource.toFilter()
                        ),
                        isSelfUserUnderLegalHoldFlow,
                        audioMessagePlayer.playingAudioMessageFlow
                    ) { conversations, isSelfUserUnderLegalHold, playingAudioMessage ->
                        conversations.map { conversationDetails ->
                            conversationDetails.toConversationItem(
                                userTypeMapper = userTypeMapper,
                                searchQuery = searchQuery,
                                selfUserTeamId = getSelfUser()?.teamId,
                                playingAudioMessage = playingAudioMessage
                            ).hideIndicatorForSelfUserUnderLegalHold(isSelfUserUnderLegalHold)
                        } to searchQuery
                    }
                }
                .map { (conversationItems, searchQuery) ->
                    if (searchQuery.isEmpty()) {
                        conversationItems.withSections(source = conversationsSource).toImmutableMap()
                    } else {
                        searchConversation(
                            conversationDetails = conversationItems,
                            searchQuery = searchQuery
                        ).withSections(source = conversationsSource).toImmutableMap()
                    }
                }
                .flowOn(dispatcher.io())
                .collect {
                    conversationListState = ConversationListState.NotPaginated(
                        isLoading = false,
                        conversations = it,
                        domain = currentAccount.domain
                    )
                }
        }
    }

    override fun searchQueryChanged(searchQuery: String) {
        viewModelScope.launch {
            searchQueryFlow.emit(searchQuery)
        }
    }

    override suspend fun refreshMissingMetadata() {
        viewModelScope.launch {
            refreshUsersWithoutMetadata()
            refreshConversationsWithoutMetadata()
        }
    }

    override fun playPauseCurrentAudio() {
        viewModelScope.launch {
            audioMessagePlayer.resumeOrPauseCurrentAudioMessage()
        }
    }

    override fun stopCurrentAudio() {
        viewModelScope.launch {
            audioMessagePlayer.forceToStopCurrentAudioMessage()
        }
    }

    @Suppress("MultiLineIfElse")
    private suspend fun clearContentSnackbarResult(
        clearContentResult: ClearConversationContentUseCase.Result,
        conversationTypeDetail: ConversationTypeDetail
    ) {
        if (conversationTypeDetail is ConversationTypeDetail.Connection) {
            throw IllegalStateException("Unsupported conversation type to clear content, something went wrong?")
        }

        val isGroup = conversationTypeDetail is ConversationTypeDetail.Group

        if (clearContentResult is ClearConversationContentUseCase.Result.Failure) {
            _infoMessage.emit(HomeSnackBarMessage.ClearConversationContentFailure(isGroup))
        } else {
            _infoMessage.emit(HomeSnackBarMessage.ClearConversationContentSuccess(isGroup))
        }
    }
}

fun Conversation.LegalHoldStatus.showLegalHoldIndicator() = this == Conversation.LegalHoldStatus.ENABLED

private fun ConversationsSource.toFilter(): ConversationFilter = when (this) {
    ConversationsSource.MAIN -> ConversationFilter.All
    ConversationsSource.ARCHIVE -> ConversationFilter.All
    ConversationsSource.GROUPS -> ConversationFilter.Groups
    ConversationsSource.CHANNELS -> ConversationFilter.Channels
    ConversationsSource.FAVORITES -> ConversationFilter.Favorites
    ConversationsSource.ONE_ON_ONE -> ConversationFilter.OneOnOne
    is ConversationsSource.FOLDER -> ConversationFilter.Folder(folderId = folderId, folderName = folderName)
}

/**
 * If self user is under legal hold then we shouldn't show legal hold indicator next to every conversation as in that case
 * the legal hold indication is shown in the header of the conversation list for self user in that case and it's enough.
 */
private fun ConversationItem.hideIndicatorForSelfUserUnderLegalHold(isSelfUserUnderLegalHold: Boolean) =
    when (isSelfUserUnderLegalHold) {
        true -> when (this) {
            is ConversationItem.ConnectionConversation -> this.copy(showLegalHoldIndicator = false)
            is ConversationItem.Group.Regular -> this.copy(showLegalHoldIndicator = false)
            is ConversationItem.Group.Channel -> this.copy(showLegalHoldIndicator = false)
            is ConversationItem.PrivateConversation -> this.copy(showLegalHoldIndicator = false)
        }

        else -> this
    }

@Suppress("ComplexMethod")
private fun List<ConversationItem>.withSections(source: ConversationsSource): Map<ConversationSection, List<ConversationItem>> {
    return when (source) {
        ConversationsSource.ARCHIVE -> {
            buildMap {
                if (this@withSections.isNotEmpty()) {
                    put(ConversationSection.WithoutHeader, this@withSections)
                }
            }
        }

        ConversationsSource.FAVORITES,
        ConversationsSource.GROUPS,
        ConversationsSource.CHANNELS,
        ConversationsSource.ONE_ON_ONE,
        is ConversationsSource.FOLDER,
        ConversationsSource.MAIN -> {
            val (unreadConversations, remainingConversations) = unreadToReadConversationsItems()
            buildMap {
                if (unreadConversations.isNotEmpty()) {
                    put(ConversationSection.Predefined.NewActivities, unreadConversations)
                }
                if (remainingConversations.isNotEmpty()) {
                    put(ConversationSection.Predefined.Conversations, remainingConversations)
                }
            }
        }

        is ConversationsSource.CHANNELS -> {
            val (unreadConversations, remainingConversations) = unreadToReadConversationsItems()
            buildMap {
                put(ConversationSection.Predefined.BrowseChannels, emptyList())
                if (unreadConversations.isNotEmpty()) {
                    put(ConversationSection.Predefined.NewActivities, unreadConversations)
                }
                if (remainingConversations.isNotEmpty()) {
                    put(ConversationSection.Predefined.Conversations, remainingConversations)
                }
            }
        }
    }
}

@Suppress("CyclomaticComplexMethod")
private fun List<ConversationItem>.unreadToReadConversationsItems(): Pair<List<ConversationItem>, List<ConversationItem>> {
    val unreadConversations = filter {
        when (it.mutedStatus) {
            MutedConversationStatus.AllAllowed -> when (it.badgeEventType) {
                BadgeEventType.Blocked -> false
                BadgeEventType.Deleted -> false
                BadgeEventType.Knock -> true
                BadgeEventType.MissedCall -> true
                BadgeEventType.None -> false
                BadgeEventType.ReceivedConnectionRequest -> true
                BadgeEventType.SentConnectRequest -> false
                BadgeEventType.UnreadMention -> true
                is BadgeEventType.UnreadMessage -> true
                BadgeEventType.UnreadReply -> true
            }

            MutedConversationStatus.OnlyMentionsAndRepliesAllowed ->
                when (it.badgeEventType) {
                    BadgeEventType.UnreadReply -> true
                    BadgeEventType.UnreadMention -> true
                    BadgeEventType.ReceivedConnectionRequest -> true
                    else -> false
                }

            MutedConversationStatus.AllMuted -> false
        } || (it is ConversationItem.Group && it.hasOnGoingCall)
    }

    val remainingConversations = this - unreadConversations.toSet()
    return unreadConversations to remainingConversations
}

private fun searchConversation(conversationDetails: List<ConversationItem>, searchQuery: String): List<ConversationItem> =
    conversationDetails.filter { details ->
        when (details) {
            is ConversationItem.ConnectionConversation -> details.conversationInfo.name.contains(searchQuery, true)
            is ConversationItem.Group -> details.groupName.contains(searchQuery, true)
            is ConversationItem.PrivateConversation -> details.conversationInfo.name.contains(searchQuery, true)
        }
    }
