package com.wire.android.core.crypto.model

import com.wire.android.shared.user.QualifiedId

data class CryptoSessionId(val userId: QualifiedId, val cryptoClientId: CryptoClientId) {
    //TODO Take domain into consideration here too
    val value: String = "${userId.id}_${cryptoClientId}"
}
