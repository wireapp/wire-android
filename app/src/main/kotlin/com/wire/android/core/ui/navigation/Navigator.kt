package com.wire.android.core.ui.navigation

import com.wire.android.feature.auth.login.ui.navigation.LoginNavigator
import com.wire.android.feature.auth.registration.ui.navigation.CreateAccountNavigator

class Navigator(
    val createAccount: CreateAccountNavigator,
    val login: LoginNavigator
)
