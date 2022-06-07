package com.wire.android.ui.home.gallery

import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages

data class MediaGalleryViewState(
    val screenTitle: String? = null,
    val onSnackbarMessage: MediaGallerySnackbarMessages? = null,
)
