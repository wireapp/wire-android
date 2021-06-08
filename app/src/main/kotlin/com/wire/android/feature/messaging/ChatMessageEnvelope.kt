package com.wire.android.feature.messaging

data class ChatMessageEnvelope(
    val senderClientId: String,
    val recipients: List<RecipientEntry>,
    val dataBlob: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is ChatMessageEnvelope
                && other.senderClientId == senderClientId
                && other.recipients == recipients
                && other.dataBlob.contentEquals(dataBlob))

    override fun hashCode(): Int {
        var result = senderClientId.hashCode()
        result = 31 * result + recipients.hashCode()
        result = 31 * result + (dataBlob?.contentHashCode() ?: 0)
        return result
    }
}

data class RecipientEntry(val userId: String, val clientPayloads: List<ClientPayload>)

data class ClientPayload(val clientId: String, val payload: ByteArray) {

    override fun equals(other: Any?): Boolean =
        this === other || (other is ClientPayload
                && other.clientId == clientId
                && other.payload.contentEquals(payload))

    override fun hashCode(): Int = 31 * clientId.hashCode() + payload.contentHashCode()

}
