package com.wire.android.feature.auth.registration.ui.navigation

import androidx.fragment.app.FragmentActivity
import com.wire.android.core.ui.navigation.FragmentStackHandler
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountCodeFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountEmailFragment

class CreateAccountNavigator(private val fragmentStackHandler: FragmentStackHandler) {

    fun openEmailScreen(activity: FragmentActivity) {
        fragmentStackHandler.replaceFragment(activity, CreatePersonalAccountEmailFragment.newInstance())
    }

    fun openCodeScreen(activity: FragmentActivity, email: String) {
        fragmentStackHandler.replaceFragment(activity, CreatePersonalAccountCodeFragment.newInstance(email))
    }
}
