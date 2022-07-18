package com.wire.android.ui.home.conversations.details.options

data class GroupConversationOptionsState (
    val groupName: String = "",
    val isTeamGroup: Boolean = false,
    val isGuestAllowed: Boolean = false,
    val isServicesAllowed: Boolean = false,
    val isUpdatingAllowed: Boolean = false,
    val isUpdatingGuestAllowed: Boolean = false,
    val isGuestUpdateDialogShown: Boolean = false,
    val error: Error = Error.None
) {
    sealed interface Error {
        object None : Error
        class UpdateGuestError(val cause: CoreFailure) : Error
        class UpdateServicesError(val cause: CoreFailure) : Error
    }
}
