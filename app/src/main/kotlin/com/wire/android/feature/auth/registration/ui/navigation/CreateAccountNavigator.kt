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
import com.wire.android.feature.auth.registration.pro.email.CreateProAccountTeamEmailActivity
import com.wire.android.feature.auth.registration.pro.team.CreateProAccountTeamNameFragment

class CreateAccountNavigator(
    private val fragmentStackHandler: FragmentStackHandler,
    private val uriNavigationHandler: UriNavigationHandler
) {

    fun openCreateAccount(context: Context) = context.startActivity(CreateAccountActivity.newIntent(context))

    fun openPersonalEmailScreen(activity: FragmentActivity) {
        fragmentStackHandler.replaceFragment(activity, CreatePersonalAccountEmailFragment.newInstance())
    }

    fun openPersonalCodeScreen(activity: FragmentActivity, email: String) {
        fragmentStackHandler.replaceFragment(activity, CreatePersonalAccountCodeFragment.newInstance(email))
    }

    fun openPersonalNameScreen(activity: FragmentActivity, email: String, activationCode: String) {
        fragmentStackHandler.replaceFragment(activity,
            CreatePersonalAccountNameFragment.newInstance(email = email, activationCode = activationCode))
    }

    fun openPersonalPasswordScreen(activity: FragmentActivity, name: String, email: String, activationCode: String) {
        fragmentStackHandler.replaceFragment(activity, CreatePersonalAccountPasswordFragment.newInstance(name, email, activationCode))
    }

    fun openProTeamNameScreen(activity: FragmentActivity) {
        fragmentStackHandler.replaceFragment(activity, CreateProAccountTeamNameFragment.newInstance())
    }

    fun openProTeamEmailScreen(context: Context) =
        context.startActivity(CreateProAccountTeamEmailActivity.newIntent(context))

    fun openProAboutTeamScreen(context: Context) =
        uriNavigationHandler.openUri(context, "$CONFIG_URL$TEAM_ABOUT_URL_SUFFIX")

    companion object {
        //TODO need to get the url prefix from Config (default.json)
        private const val CONFIG_URL = "https://wire.com"
        private const val TEAM_ABOUT_URL_SUFFIX = "/products/pro-secure-team-collaboration/"
    }
}
