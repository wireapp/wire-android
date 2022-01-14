package com.wire.android.ui.conversation.model

data class Conversation(
    val name: String,
    val memberShip: Membership = Membership.None,
    val isLegalHold: Boolean = false
)

enum class Membership(val label: String) {
    Quest("Quest"), External("External"), None("")
}


