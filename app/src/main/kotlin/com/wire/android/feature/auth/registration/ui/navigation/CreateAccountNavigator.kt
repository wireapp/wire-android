package com.wire.android.feature.auth.registration.ui.navigation

import androidx.fragment.app.FragmentActivity
import com.wire.android.core.ui.navigation.FragmentNavigator
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountCodeFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountEmailFragment

class CreateAccountNavigator(private val fragmentNavigator: FragmentNavigator) {

    fun openEmailScreen(activity: FragmentActivity) =
        fragmentNavigator.openFragment(activity, CreatePersonalAccountEmailFragment.newInstance())

    fun openCodeScreen(activity: FragmentActivity, email: String) =
        fragmentNavigator.openFragment(activity, CreatePersonalAccountCodeFragment.newInstance(email))
}
