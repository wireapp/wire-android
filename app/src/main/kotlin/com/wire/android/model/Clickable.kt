package com.wire.android.model

data class Clickable(
    val enabled: Boolean = true,
    val blockUntilSynced: Boolean = false,
    val onLongClick: (() -> Unit)? = null,
    val onClick: () -> Unit = {}
)
