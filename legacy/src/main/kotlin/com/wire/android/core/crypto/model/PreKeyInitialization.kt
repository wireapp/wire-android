package com.wire.android.core.crypto.model

data class PreKeyInitialization(
    val createdKeys: List<PreKey>,
    val lastKey: PreKey
)
