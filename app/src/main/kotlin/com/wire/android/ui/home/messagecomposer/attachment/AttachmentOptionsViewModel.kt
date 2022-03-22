package com.wire.android.ui.home.messagecomposer.attachment

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.conversations.AttachmentPart
import com.wire.android.util.DEFAULT_FILE_MIME_TYPE
import com.wire.android.util.getMimeType
import com.wire.android.util.orDefault
import com.wire.android.util.toByteArray
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AttachmentOptionsViewModel @Inject constructor() : ViewModel() {

    var attachmentState by mutableStateOf<AttachmentState>(AttachmentState.Initial)
        private set

    fun prepareAttachment(context: Context, attachmentUri: Uri) {
        viewModelScope.launch {
            val attachment =
                AttachmentPart(attachmentUri.getMimeType(context).orDefault(DEFAULT_FILE_MIME_TYPE), attachmentUri.toByteArray(context))
            attachmentState = AttachmentState.Picked(attachment)
        }
    }
}

sealed class AttachmentState {
    object Initial : AttachmentState()
    class Picked(val attachmentPart: AttachmentPart) : AttachmentState()
    object Error : AttachmentState()
}
