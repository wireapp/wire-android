package com.wire.android.feature.conversation

sealed class ConversationType

object Group : ConversationType()
object Self : ConversationType()
object OneToOne : ConversationType()
object WaitingForConnection : ConversationType()
object IncomingConnection : ConversationType()
object Unknown : ConversationType()
