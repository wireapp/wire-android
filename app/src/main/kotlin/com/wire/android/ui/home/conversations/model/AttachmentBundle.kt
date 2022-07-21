package com.wire.android.ui.home.conversations.model

import okio.Path

/**
 * Represents an attachment part of a message to be sent
 */
data class AttachmentBundle(
    val mimeType: String,
    val dataPath: Path,
    val dataSize: Long,
    val fileName: String,
    val attachmentType: AttachmentType
)

enum class AttachmentType {
    // TODO: Add audio or video later on
    IMAGE, GENERIC_FILE
}
