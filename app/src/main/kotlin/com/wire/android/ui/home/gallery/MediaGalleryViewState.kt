package com.wire.android.ui.home.gallery

import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState

data class MediaGalleryViewState(
    val screenTitle: String? = null,
    val onSnackbarMessage: MediaGallerySnackbarMessages? = null,
    val deleteMessageDialogsState: DeleteMessageDialogsState = DeleteMessageDialogsState.States(
        forYourself = DeleteMessageDialogActiveState.Hidden,
        forEveryone = DeleteMessageDialogActiveState.Hidden
    )
)
