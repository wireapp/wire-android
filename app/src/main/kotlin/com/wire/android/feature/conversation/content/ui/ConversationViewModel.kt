package com.wire.android.feature.conversation.content.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.conversation.ConversationID
import com.wire.android.feature.conversation.content.usecase.GetConversationUseCase
import com.wire.android.feature.conversation.content.usecase.GetConversationUseCaseParams
import com.wire.android.feature.conversation.content.usecase.SendTextMessageUseCase
import com.wire.android.feature.conversation.content.usecase.SendTextMessageUseCaseParams
import com.wire.android.feature.conversation.usecase.ResetCurrentConversationIdUseCase
import com.wire.android.feature.conversation.usecase.UpdateCurrentConversationIdUseCase
import com.wire.android.feature.conversation.usecase.UpdateCurrentConversationUseCaseParams

class ConversationViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val getConversationUseCase: GetConversationUseCase,
    private val updateCurrentConversationIdUseCase: UpdateCurrentConversationIdUseCase,
    private val resetCurrentConversationIdUseCase: ResetCurrentConversationIdUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _conversationIdLiveData: MutableLiveData<ConversationID> = MutableLiveData()
    val conversationIdLiveData: LiveData<ConversationID> = _conversationIdLiveData

    private val _conversationMessagesLiveData = MutableLiveData<List<CombinedMessageContact>>()
    val conversationMessagesLiveData: LiveData<List<CombinedMessageContact>> = _conversationMessagesLiveData

    fun cacheConversationId(conversationId: ConversationID) {
        _conversationIdLiveData.value = conversationId
    }

    fun fetchMessages(conversationId: ConversationID) {
        val params = GetConversationUseCaseParams(conversationId = conversationId)
        getConversationUseCase(viewModelScope, params) {
            _conversationMessagesLiveData.value = it
        }
    }

    fun sendTextMessage(textMessage: String) {
        sendTextMessageUseCase(viewModelScope, SendTextMessageUseCaseParams(_conversationIdLiveData.value!!, textMessage)) { }
    }

    fun updateCurrentConversationId(conversationId: ConversationID) {
        val params = UpdateCurrentConversationUseCaseParams(conversationId = conversationId)
        updateCurrentConversationIdUseCase(viewModelScope, params)
    }

    fun resetCurrentConversationId() {
        resetCurrentConversationIdUseCase(viewModelScope, Unit)
    }
}
