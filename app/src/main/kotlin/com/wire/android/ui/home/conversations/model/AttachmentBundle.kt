package com.wire.android.ui.home.conversations.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents an attachment part of a message to be sent
 */
@Parcelize
data class AttachmentBundle(val mimeType: String, val rawContent: ByteArray) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttachmentBundle

        if (mimeType != other.mimeType) return false
        if (!rawContent.contentEquals(other.rawContent)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mimeType.hashCode()
        result = 31 * result + rawContent.contentHashCode()
        return result
    }
}
