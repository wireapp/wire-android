package com.wire.android.model

data class Clickable(
    val enabled: Boolean = true,
    val onClick: () -> Unit
)
