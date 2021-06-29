package com.wire.android.core.ui.navigation

import com.wire.android.feature.auth.login.ui.navigation.LoginNavigator
import com.wire.android.feature.auth.registration.ui.navigation.CreateAccountNavigator
import com.wire.android.feature.conversation.list.ui.navigation.MainNavigator
import com.wire.android.feature.profile.ui.ProfileNavigator
import com.wire.android.feature.welcome.ui.navigation.WelcomeNavigator

class Navigator(
    val welcome: WelcomeNavigator,
    val createAccount: CreateAccountNavigator,
    val login: LoginNavigator,
    val main: MainNavigator,
    val profile: ProfileNavigator
)
