package com.wire.android.core.crypto.model

import com.wire.android.shared.user.QualifiedId

data class CryptoSessionId(val userId: QualifiedId, val cryptoClientId: CryptoClientId) {
    val value: String = "${userId}_${cryptoClientId}"
}
