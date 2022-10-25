package com.wire.android.ui.home.conversations.details.options

import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
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
    val changeGuestOptionConfirmationRequired: Boolean = false,
    val changeServiceOptionConfirmationRequired: Boolean = false,
    val loadingGuestOption: Boolean = false,
    val loadingServicesOption: Boolean = false,
    val error: Error = Error.None,
) {

    sealed interface Error {
        object None : Error
        class UpdateGuestError(val cause: CoreFailure) : Error
        class UpdateServicesError(val cause: CoreFailure) : Error
    }
}
