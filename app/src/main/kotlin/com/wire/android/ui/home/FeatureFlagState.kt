package com.wire.android.ui.home

data class FeatureFlagState(
    val showFileSharingDialog: Boolean = false,
    val isFileSharingEnabledState: Boolean = true,
    val showFileSharingRestrictedDialog: Boolean = false,
    val openImportMediaScreen: Boolean = false
)
