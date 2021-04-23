package com.wire.android.crypto.mapper

import android.util.Base64
import com.wire.android.crypto.model.PreKey

class PreKeyMapper {

    fun toCryptoBoxModel(data: PreKey): com.wire.cryptobox.PreKey {
        val decoded = Base64.decode(data.encodedData, Base64.NO_WRAP)
        return com.wire.cryptobox.PreKey(data.id, decoded)
    }

    fun fromCryptoBoxModel(model: com.wire.cryptobox.PreKey): PreKey {
        val encoded = Base64.encode(model.data, Base64.NO_WRAP)
        return PreKey(model.id, encoded.decodeToString())
    }
}
