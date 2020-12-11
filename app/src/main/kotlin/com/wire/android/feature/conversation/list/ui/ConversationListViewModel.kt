package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventsHandler
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationListPagingDelegate
import com.wire.android.feature.conversation.list.usecase.GetConversationsParams
import com.wire.android.feature.conversation.list.usecase.GetConversationsUseCase
import com.wire.android.shared.auth.activeuser.GetActiveUserUseCase

class ConversationListViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val getActiveUserUseCase: GetActiveUserUseCase,
    private val getConversationsUseCase: GetConversationsUseCase,
    conversationListPagingDelegate: ConversationListPagingDelegate,
    private val eventsHandler: EventsHandler
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _userNameLiveData = MutableLiveData<String>()
    val userNameLiveData: LiveData<String> = _userNameLiveData

    private val _conversationListErrorLiveData = SingleLiveEvent<Failure>()
    val conversationListErrorLiveData: LiveData<Failure> = _conversationListErrorLiveData

    val conversationListItemsLiveData: LiveData<PagedList<ConversationListItem>> =
        conversationListPagingDelegate.conversationList(CONVERSATIONS_PAGE_SIZE, ::getConversationListNextPage)

    fun fetchUserName() {
        getActiveUserUseCase(viewModelScope, Unit) {
            it.onSuccess { user -> _userNameLiveData.value = user.name }
        }
    }

    private fun getConversationListNextPage(lastItemLoaded: ConversationListItem?) =
        getConversations(lastItemLoaded?.id)

    private fun getConversations(start: String?) {
        val params = GetConversationsParams(start, CONVERSATIONS_PAGE_SIZE)
        getConversationsUseCase(viewModelScope, params) { result ->
            result.onSuccess(::getConversationMembers)
                .onFailure {
                    _conversationListErrorLiveData.value = it
                }
        }
    }

    private fun getConversationMembers(conversations: List<Conversation>) {
        //TODO: call use case
    }

    fun subscribeToEvents() = with(eventsHandler) {
        subscribe<Event.UsernameChanged> { _userNameLiveData.value = it.username }
        subscribe<Event.ConversationNameChanged> { TODO() }
    }

    companion object {
        private const val CONVERSATIONS_PAGE_SIZE = 30
    }
}
