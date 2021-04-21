package com.wire.android.crypto.mapper

interface CryptoBoxMapper<A,B> {

    fun toCryptoBoxModel(data: A): B

    fun fromCryptoBoxModel(model: B): A

}
