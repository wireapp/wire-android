package com.wire.android.core.crypto.mapper

import android.util.Base64
import com.wire.android.core.crypto.model.PreKey
import com.wire.cryptobox.PreKey as CryptoPreKey

class PreKeyMapper {

    fun toCryptoBoxModel(data: PreKey): CryptoPreKey {
        val decoded = Base64.decode(data.encodedData, Base64.NO_WRAP)
        return CryptoPreKey(data.id, decoded)
    }

    fun fromCryptoBoxModel(model: CryptoPreKey): PreKey {
        val encoded = Base64.encode(model.data, Base64.NO_WRAP)
        return PreKey(model.id, encoded.decodeToString())
    }
}
