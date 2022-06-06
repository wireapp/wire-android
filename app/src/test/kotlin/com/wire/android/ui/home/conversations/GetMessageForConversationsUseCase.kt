//package com.wire.android.ui.home.conversations
//
//import com.wire.android.config.TestDispatcherProvider
//import com.wire.android.navigation.NavigationManager
//import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
//import com.wire.kalium.logic.feature.asset.SendAssetMessageUseCase
//import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
//import io.mockk.every
//import io.mockk.impl.annotations.MockK
//import kotlinx.coroutines.test.TestDispatcher
//import kotlinx.coroutines.test.runTest
//import org.amshove.kluent.internal.assertEquals
//import org.junit.jupiter.api.Test
//
//class GetMessageForConversationsUseCaseTest {
//
//    @MockK
//    lateinit var navigationManager: NavigationManager
//
//    @MockK
//    lateinit var sendTextMessage: SendTextMessageUseCase
//
//    @MockK
//    lateinit var sendAssetMessage: SendAssetMessageUseCase
//SendAssetMessageUseCase
//
//    private val getMessagesForConversationUseCase =
//        GetMessagesForConversationUseCase(getMessages, observeMemberDetails, messageMapper, TestDispatcher())
////
////    @Test
////    fun `given message sent by another user, when solving the message header, then the state should contain that user name`() = runTest {
////        // Given
////        val senderId = UserId("value", "domain")
////        val messages = listOf(mockedMessage(senderId = senderId))
////        val otherUserName = "other user"
////
////
////        val otherMember = mockOtherUserDetails(otherUserName, senderId)
////        val (arrangement, viewModel) = Arrangement()
////            .withChannelUpdates(messages, listOf(otherMember))
////            .arrange()
////
////        // When - Then
////        every { arrangement.uiText.asString(any()) } returns (otherUserName)
////        assertEquals(otherUserName, viewModel.conversationViewState.messages.first().messageHeader.username.asString(arrangement.context))
////    }
//
//
//}
