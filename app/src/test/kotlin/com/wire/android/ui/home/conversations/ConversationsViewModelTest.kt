package com.wire.android.ui.home.conversations

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.conversation.GetConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.GetRecentMessagesUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
@ExtendWith(CoroutineTestExtension::class)
class ConversationsViewModelTest {
    private lateinit var conversationsViewModel: ConversationViewModel

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var getMessages: GetRecentMessagesUseCase

    @MockK
    lateinit var sendTextMessage: SendTextMessageUseCase


    @MockK
    lateinit var deleteMessage: DeleteMessageUseCase

    @MockK
    lateinit var getConversationDetails: GetConversationDetailsUseCase

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { savedStateHandle.getLiveData<String>(any()) } returns MutableLiveData("")
        every { savedStateHandle.set(any(), any<String>()) } returns Unit

        conversationsViewModel = ConversationViewModel(
            savedStateHandle = savedStateHandle,
            navigationManager = navigationManager,
            getMessages = getMessages,
            getConversationDetails = getConversationDetails,
            sendTextMessage = sendTextMessage,
            deleteMessage = deleteMessage
        )
    }

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible`() {
        conversationsViewModel.showDeleteMessageDialog("")
        conversationsViewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden,
            forEveryone = DeleteMessageDialogActiveState.Visible("", conversationsViewModel.conversationId!!)
        )
    }

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageForYourselfDialog is visible`() {
        conversationsViewModel.showDeleteMessageForYourselfDialog("")
        conversationsViewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Visible("", conversationsViewModel.conversationId!!),
            forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    }


    @Test
    fun `validate deleteMessageDialogsState states when dialogs are dismissed`() {
        conversationsViewModel.onDialogDismissed()
        conversationsViewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden,
            forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    }
}
