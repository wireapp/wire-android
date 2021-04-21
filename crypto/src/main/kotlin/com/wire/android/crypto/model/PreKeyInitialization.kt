package com.wire.android.crypto.model

data class PreKeyInitialization(
    val createdKeys: List<PreKey>,
    val lastKey: PreKey
)
