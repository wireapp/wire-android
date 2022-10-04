package com.wire.android.ui.home.conversations.details.options

import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

data class GroupConversationOptionsState(
    val conversationId: ConversationId,
    val groupName: String = "",
    val protocolInfo: Conversation.ProtocolInfo = Conversation.ProtocolInfo.Proteus,
    val areAccessOptionsAvailable: Boolean = false,
    val isGuestAllowed: Boolean = false,
    val isServicesAllowed: Boolean = false,
    val isUpdatingAllowed: Boolean = false,
    val isUpdatingGuestAllowed: Boolean = false,
//    val isAbleToRemoveGroup: Boolean = true,
    val changeGuestOptionConfirmationRequired: Boolean = false,
    val changeServiceOptionConfirmationRequired: Boolean = false,
    val loadingGuestOption: Boolean = false,
    val loadingServicesOption: Boolean = false,
//    val isSelfUserMember: Boolean = true,
    val error: Error = Error.None,
    private val conversationSheetContent: ConversationSheetContent? = null,
    val bottomSheetContentState: BottomSheetContent? = null,
) {

    fun setBottomSheetStateToConversation(): GroupConversationOptionsState =
        conversationSheetContent?.let { copy(bottomSheetContentState = BottomSheetContent.Conversation(it)) } ?: this

    fun setBottomSheetStateToMuteOptions(): GroupConversationOptionsState =
        conversationSheetContent?.let { copy(bottomSheetContentState = BottomSheetContent.Mute(it)) } ?: this

    fun updateMuteStatus(status: MutedConversationStatus): GroupConversationOptionsState {
        return conversationSheetContent?.let {
            val newConversationSheetContent = conversationSheetContent.copy(mutingConversationState = status)
            val newBottomSheetContentState = when (bottomSheetContentState) {
                is BottomSheetContent.Mute -> bottomSheetContentState.copy(
                    conversationData = bottomSheetContentState.conversationData.copy(mutingConversationState = status)
                )
                is BottomSheetContent.Conversation -> bottomSheetContentState.copy(
                    conversationData = bottomSheetContentState.conversationData.copy(mutingConversationState = status)
                )
                null -> null
            }
            copy(conversationSheetContent = newConversationSheetContent, bottomSheetContentState = newBottomSheetContentState)
        } ?: this
    }

    fun clearBottomSheetState(): GroupConversationOptionsState =
        copy(bottomSheetContentState = null)

    sealed interface Error {
        object None : Error
        class UpdateGuestError(val cause: CoreFailure) : Error
        class UpdateServicesError(val cause: CoreFailure) : Error
    }
}

sealed class BottomSheetContent {
    data class Conversation(val conversationData: ConversationSheetContent) : BottomSheetContent()
    data class Mute(val conversationData: ConversationSheetContent) : BottomSheetContent()
}
