package com.wire.android.crypto.mapper

import android.util.Base64
import com.wire.android.crypto.model.PreKey

class PreKeyMapper : CryptoBoxMapper<PreKey, com.wire.cryptobox.PreKey> {

    override fun toCryptoBoxModel(data: PreKey): com.wire.cryptobox.PreKey {
        val decoded = Base64.decode(data.encodedData, Base64.NO_WRAP)
        return com.wire.cryptobox.PreKey(data.id, decoded)
    }

    override fun fromCryptoBoxModel(model: com.wire.cryptobox.PreKey): PreKey {
        val encoded = Base64.encode(model.data, Base64.NO_WRAP)
        return PreKey(model.id, encoded.decodeToString())
    }
}
