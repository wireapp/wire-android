package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventsHandler
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.shared.user.usecase.GetCurrentUserUseCase

class ConversationListViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    conversationListPagingDelegate: ConversationListPagingDelegate,
    private val eventsHandler: EventsHandler
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _userNameLiveData = MutableLiveData<String>()
    val userNameLiveData: LiveData<String> = _userNameLiveData

    val conversationListItemsLiveData: LiveData<PagedList<ConversationListItem>> =
        conversationListPagingDelegate.conversationList(CONVERSATIONS_PAGE_SIZE)

    fun fetchUserName() {
        getCurrentUserUseCase(viewModelScope, Unit) {
            it.onSuccess { user -> _userNameLiveData.value = user.name }
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
