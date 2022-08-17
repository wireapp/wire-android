package com.wire.android.ui.home.conversations.details.options

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId

data class GroupConversationOptionsState(
    val conversationId: ConversationId,
    val groupName: String = "",
    val areAccessOptionsAvailable: Boolean = false,
    val isGuestAllowed: Boolean = false,
    val isServicesAllowed: Boolean = false,
    val isUpdatingAllowed: Boolean = false,
    val isUpdatingGuestAllowed: Boolean = false,
    val isAbleToRemoveGroup: Boolean = true,
    val changeGuestOptionConfirmationRequired: Boolean = false,
    val loadingGuestOption: Boolean = false,
    val loadingServicesOption: Boolean = false,
    val error: Error = Error.None
) {
    sealed interface Error {
        object None : Error
        class UpdateGuestError(val cause: CoreFailure) : Error
        class UpdateServicesError(val cause: CoreFailure) : Error
    }
}
