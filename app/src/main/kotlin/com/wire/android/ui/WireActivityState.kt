package com.wire.android.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import com.wire.android.util.deeplink.SSOFailureCodes

data class WireActivityState(
    val ssoErrorDialog: WireActivityError = WireActivityError.None
)

sealed class WireActivityError {
    object None: WireActivityError()
    sealed class DialogError: WireActivityError() {
        data class SSOError @OptIn(ExperimentalMaterial3Api::class) constructor(val coreFailure: SSOFailureCodes): DialogError()
    }
}
