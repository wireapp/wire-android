package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventsHandler
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.list.usecase.GetConversationsParams
import com.wire.android.feature.conversation.list.usecase.GetConversationsUseCase
import com.wire.android.shared.auth.activeuser.GetActiveUserUseCase

class ConversationListViewModel(
    private val getActiveUserUseCase: GetActiveUserUseCase,
    private val getConversationsUseCase: GetConversationsUseCase,
    private val eventsHandler: EventsHandler
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor() {

    private val _userNameLiveData = MutableLiveData<String>()
    val userNameLiveData: LiveData<String> = _userNameLiveData

    private val _conversationsLiveData = MutableLiveData<Either<Failure, PagedList<Conversation>>>()
    val conversationsLiveData: LiveData<Either<Failure, PagedList<Conversation>>> = _conversationsLiveData

    fun fetchUserName() {
        getActiveUserUseCase(viewModelScope, Unit) {
            it.onSuccess { user -> _userNameLiveData.value = user.name }
        }
    }

    fun fetchConversations() {
        getConversationsUseCase(viewModelScope, GetConversationsParams(size = CONVERSATIONS_PAGE_SIZE)) {
            _conversationsLiveData.value = it
        }
    }

    fun subscribeToEvents() = with(eventsHandler) {
        subscribe<Event.UsernameChanged> { _userNameLiveData.value = it.username }
        subscribe<Event.ConversationNameChanged> { TODO() }
    }

    companion object {
        private const val CONVERSATIONS_PAGE_SIZE = 30
    }
}
