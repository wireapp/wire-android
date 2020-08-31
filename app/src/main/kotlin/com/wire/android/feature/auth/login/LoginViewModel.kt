package com.wire.android.feature.auth.login

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.wire.android.core.extension.domainAddress
import com.wire.android.core.network.BackendConfig

class LoginViewModel(private val backendConfig: BackendConfig) : ViewModel() {

    val forgotPasswordUri by lazy {
        Uri.Builder().domainAddress(backendConfig.accountsUrl).appendPath(FORGOT_PASSWORD_PATH).build()
    }

    companion object {
        private const val FORGOT_PASSWORD_PATH = "forgot"
    }
}
