package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.exception.Failure
import com.wire.android.core.extension.failure
import com.wire.android.core.extension.success
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.conversation.list.usecase.Conversation
import com.wire.android.feature.conversation.list.usecase.GetConversationsParams
import com.wire.android.feature.conversation.list.usecase.GetConversationsUseCase
import com.wire.android.shared.auth.activeuser.GetActiveUserUseCase

class ConversationListViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val getActiveUserUseCase: GetActiveUserUseCase,
    private val getConversationsUseCase: GetConversationsUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _userNameLiveData = MutableLiveData<String>()
    val userNameLiveData: LiveData<String> = _userNameLiveData

    private val _conversationsLiveData = MutableLiveData<Either<Failure, List<Conversation>>>()
    val conversationsLiveData: LiveData<Either<Failure, List<Conversation>>> = _conversationsLiveData

    fun fetchUserName() {
        getActiveUserUseCase(viewModelScope, Unit) {
            it.onSuccess { user -> _userNameLiveData.value = user.name }
        }
    }

    fun fetchConversations() {
        val params = GetConversationsParams(size = 10)
        getConversationsUseCase(viewModelScope, params) {
            it.onSuccess { conversations ->
                _conversationsLiveData.success(conversations)
            }.onFailure { failure ->
                _conversationsLiveData.failure(failure)
            }
        }
    }
}
