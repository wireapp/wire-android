package com.wire.android.feature.auth.login.ui.navigation

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.wire.android.core.extension.clearStack
import com.wire.android.core.extension.domainAddress
import com.wire.android.core.network.NetworkConfig
import com.wire.android.core.ui.navigation.FragmentStackHandler
import com.wire.android.core.ui.navigation.UriNavigationHandler
import com.wire.android.feature.auth.client.ui.DeviceLimitActivity
import com.wire.android.feature.auth.client.ui.DeviceLimitFragment
import com.wire.android.feature.auth.login.LoginActivity

class LoginNavigator(
    private val uriNavigationHandler: UriNavigationHandler,
    private val networkConfig: NetworkConfig,
    private val fragmentStackHandler: FragmentStackHandler
) {

    private val forgotPasswordUri by lazy {
        Uri.Builder().domainAddress(networkConfig.accountsUrl).appendPath(FORGOT_PASSWORD_PATH).build()
    }

    fun openLogin(context: Context) = context.startActivity(LoginActivity.newIntent(context))

    fun openForgotPassword(context: Context) = uriNavigationHandler.openUri(context, forgotPasswordUri)

    fun openDeviceLimitScreen(context: Context, userId: String) =
        context.startActivity(DeviceLimitActivity.newIntent(context, userId).clearStack())

    fun openDeviceLimitErrorScreen(activity: FragmentActivity, addToBackStack: Boolean = false) {
        fragmentStackHandler.replaceFragment(activity, addToBackStack = addToBackStack) {
            DeviceLimitFragment.newInstance()
        }
    }

    companion object {
        private const val FORGOT_PASSWORD_PATH = "forgot"
    }
}
