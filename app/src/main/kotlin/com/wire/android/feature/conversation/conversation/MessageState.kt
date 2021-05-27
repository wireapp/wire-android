package com.wire.android.feature.conversation.conversation

sealed class MessageState
object Sent: MessageState()
object Pending: MessageState()
object Delivered: MessageState()
object Failed: MessageState()
object FailedRead: MessageState()
object Deleted: MessageState()
object Default: MessageState()
