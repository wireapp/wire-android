package com.wire.android.ui.calling.model

import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.id.QualifiedID

data class UICallParticipant(
    val id: QualifiedID,
    val clientId: String,
    val name: String = "",
    val isMuted: Boolean,
    val isSpeaking: Boolean = false,
    val avatar: ImageAsset.UserAvatarAsset? = null,
    val membership: Membership,
)
