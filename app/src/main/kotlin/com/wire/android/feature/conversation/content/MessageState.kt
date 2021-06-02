package com.wire.android.feature.conversation.content

sealed class MessageState
object Sent: MessageState()
object Pending: MessageState()
object Delivered: MessageState()
object Failed: MessageState()
object Deleted: MessageState()
object Default: MessageState()
