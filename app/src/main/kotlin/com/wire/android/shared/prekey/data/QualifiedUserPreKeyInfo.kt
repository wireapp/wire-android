package com.wire.android.shared.prekey.data

import com.wire.android.core.crypto.model.PreKey
import com.wire.android.shared.user.QualifiedId

data class QualifiedUserPreKeyInfo(val userId: QualifiedId, val clientsInfo: List<ClientPreKeyInfo>)

@Deprecated(
        "This data structure does not consider domain, needed for Federation",
        ReplaceWith("QualifiedUserPreKeyInfo")
)
data class UserPreKeyInfo(val userId: String, val clientsInfo: List<ClientPreKeyInfo>)

data class ClientPreKeyInfo(val clientId: String, val preKey: PreKey)
