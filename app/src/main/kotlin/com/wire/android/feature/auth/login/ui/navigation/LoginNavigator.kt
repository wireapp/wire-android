package com.wire.android.feature.auth.login.ui.navigation

import android.content.Context
import android.net.Uri
import com.wire.android.core.extension.domainAddress
import com.wire.android.core.network.BackendConfig
import com.wire.android.core.ui.navigation.UriNavigationHandler
import com.wire.android.feature.auth.login.LoginActivity

class LoginNavigator(
    private val uriNavigationHandler: UriNavigationHandler,
    private val backendConfig: BackendConfig
) {

    private val forgotPasswordUri by lazy {
        Uri.Builder().domainAddress(backendConfig.accountsUrl).appendPath(FORGOT_PASSWORD_PATH).build()
    }

    fun openLogin(context: Context) = context.startActivity(LoginActivity.newIntent(context))

    fun openForgotPassword(context: Context) = uriNavigationHandler.openUri(context, forgotPasswordUri)

    companion object {
        private const val FORGOT_PASSWORD_PATH = "forgot"
    }
}
