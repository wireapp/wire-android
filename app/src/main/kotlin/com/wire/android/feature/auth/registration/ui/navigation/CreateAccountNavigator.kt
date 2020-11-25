package com.wire.android.feature.auth.registration.ui.navigation

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.wire.android.core.ui.navigation.FragmentStackHandler
import com.wire.android.core.ui.navigation.UriNavigationHandler
import com.wire.android.feature.auth.registration.CreateAccountActivity
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountCodeFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountEmailFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountNameFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountPasswordFragment
import com.wire.android.feature.auth.registration.pro.email.CreateProAccountTeamEmailFragment
import com.wire.android.feature.auth.registration.pro.email.verification.CreateProAccountTeamEmailVerificationFragment
import com.wire.android.feature.auth.registration.pro.team.CreateProAccountTeamNameFragment

class CreateAccountNavigator(
    private val fragmentStackHandler: FragmentStackHandler,
    private val uriNavigationHandler: UriNavigationHandler
) {

    fun openCreateAccount(context: Context) = context.startActivity(CreateAccountActivity.newIntent(context))

    fun openPersonalAccountEmailScreen(activity: FragmentActivity) {
        fragmentStackHandler.replaceFragment(activity, CreatePersonalAccountEmailFragment.newInstance())
    }

    fun openPersonalAccountCodeScreen(activity: FragmentActivity, email: String) {
        fragmentStackHandler.replaceFragment(activity, CreatePersonalAccountCodeFragment.newInstance(email))
    }

    fun openPersonalAccountNameScreen(activity: FragmentActivity, email: String, activationCode: String) {
        fragmentStackHandler.replaceFragment(activity,
            CreatePersonalAccountNameFragment.newInstance(email = email, activationCode = activationCode))
    }

    fun openPersonalAccountPasswordScreen(activity: FragmentActivity, name: String, email: String, activationCode: String) {
        fragmentStackHandler.replaceFragment(activity, CreatePersonalAccountPasswordFragment.newInstance(name, email, activationCode))
    }

    fun openProAccountTeamNameScreen(activity: FragmentActivity) {
        fragmentStackHandler.replaceFragment(activity, CreateProAccountTeamNameFragment.newInstance())
    }

    fun openProAccountTeamEmailScreen(activity: FragmentActivity) =
        fragmentStackHandler.replaceFragment(activity, CreateProAccountTeamEmailFragment.newInstance())

    fun openProAccountTeamEmailVerificationScreen(activity: FragmentActivity, email: String) =
        fragmentStackHandler.replaceFragment(activity, CreateProAccountTeamEmailVerificationFragment.newInstance(email))

    fun openProAccountAboutTeamScreen(context: Context) =
        uriNavigationHandler.openUri(context, "$CONFIG_URL$TEAM_ABOUT_URL_SUFFIX")

    companion object {
        //TODO need to get the url prefix from Config (default.json)
        private const val CONFIG_URL = "https://wire.com"
        private const val TEAM_ABOUT_URL_SUFFIX = "/products/pro-secure-team-collaboration/"
    }
}
