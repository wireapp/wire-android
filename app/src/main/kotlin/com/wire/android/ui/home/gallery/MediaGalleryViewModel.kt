package com.wire.android.ui.home.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MediaGalleryViewModel(

) : ViewModel() {

    var mediaGalleryViewState by mutableStateOf(MediaGalleryViewState())
        private set
}
