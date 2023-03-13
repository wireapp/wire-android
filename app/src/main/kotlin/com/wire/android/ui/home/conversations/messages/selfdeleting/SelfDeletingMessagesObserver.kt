package com.wire.android.ui.home.conversations.messages.selfdeleting

import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import javax.inject.Inject

//class SelfDeletingMessagesObserve @Inject constructor() {
//
//    private val mutex = Mutex()
//
//    private val outgoingTimerSelfDeletingMessagesState: MutableStateFlow<Map<String, Long>> =
//        MutableStateFlow(emptyMap())
////
////    suspend fun observeSelfDeletingMessageProgress(messageId: String, expireAfter: Duration) {
////        mutex.withLock {
////        val isSelfDeletionOutgoing = outgoingTimerSelfDeletingMessagesState.value[messageId] != null
////        if (isSelfDeletionOutgoing) return
////
////
////        }
////        getMessageById(conversationId, messageId).map { message ->
////            require(message is Message.Ephemeral)
////
////            enqueueMessageDeletion(message)
////        }
////    }
////
////    val observableSelfDeletingMessageState: MutableSharedFlow<>
//
//
//}
