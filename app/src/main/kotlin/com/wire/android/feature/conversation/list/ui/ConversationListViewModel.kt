package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventsHandler
import androidx.paging.PagedList
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.core.ui.dialog.GeneralErrorMessage
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

    val conversationsLiveData: LiveData<Either<ErrorMessage, PagedList<Conversation>>> by lazy {
        createConversationsLiveData()
    }

    fun fetchUserName() {
        getActiveUserUseCase(viewModelScope, Unit) {
            it.onSuccess { user -> _userNameLiveData.value = user.name }
        }
    }

    private fun createConversationsLiveData() =
        getConversationsUseCase(viewModelScope, GetConversationsParams(size = 30)).map {
            mapConversationListFailure((it))
        }

    private fun mapConversationListFailure(
        result: Either<Failure, PagedList<Conversation>>
    ): Either<ErrorMessage, PagedList<Conversation>> =
        result.fold({
            Either.Left(conversationListFailureErrorMessage(it))
        }) {
            Either.Right(it)
        }!!

    //TODO: proper error handling
    private fun conversationListFailureErrorMessage(failure: Failure): ErrorMessage = GeneralErrorMessage

    fun subscribeToEvents() = with(eventsHandler) {
        subscribe<Event.UsernameChanged> { _userNameLiveData.value = it.username }
        subscribe<Event.ConversationNameChanged> { TODO() }
    }
}
