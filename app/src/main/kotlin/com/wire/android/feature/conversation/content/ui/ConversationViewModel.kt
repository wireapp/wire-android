package com.wire.android.feature.conversation.content.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.conversation.content.usecase.GetConversationUseCase
import com.wire.android.feature.conversation.content.usecase.GetConversationUseCaseParams
import com.wire.android.feature.conversation.content.usecase.SendTextMessageUseCase
import com.wire.android.feature.conversation.content.usecase.SendTextMessageUseCaseParams
import com.wire.android.feature.conversation.usecase.UpdateCurrentConversationIdUseCase
import com.wire.android.feature.conversation.usecase.UpdateCurrentConversationUseCaseParams

class ConversationViewModel(
    override val dispatcherProvider: DispatcherProvider,
    private val getConversationUseCase: GetConversationUseCase,
    private val updateCurrentConversationIdUseCase: UpdateCurrentConversationIdUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase
) : ViewModel(), UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider) {

    private val _conversationIdLiveData: MutableLiveData<String> = MutableLiveData()
    val conversationIdLiveData: LiveData<String> = _conversationIdLiveData

    private val _conversationMessagesLiveData = MutableLiveData<List<CombinedMessageContact>>()
    val conversationMessagesLiveData: LiveData<List<CombinedMessageContact>> = _conversationMessagesLiveData

    fun cacheConversationId(conversationId: String) {
        _conversationIdLiveData.value = conversationId
    }

    fun fetchMessages(conversationId: String) {
        val params = GetConversationUseCaseParams(conversationId = conversationId)
        getConversationUseCase(viewModelScope, params) {
            _conversationMessagesLiveData.value = it
        }
    }

    fun sendTextMessage(textMessage: String) {
        sendTextMessageUseCase(viewModelScope, SendTextMessageUseCaseParams(_conversationIdLiveData.value!!, textMessage)) { }
    }

    fun updateCurrentConversationId(conversationId: String) {
        val params = UpdateCurrentConversationUseCaseParams(conversationId = conversationId)
        updateCurrentConversationIdUseCase(viewModelScope, params)
    }
}
