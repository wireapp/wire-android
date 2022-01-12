package com.wire.android.feature.welcome.di

import com.wire.android.feature.welcome.ui.navigation.WelcomeNavigator
import org.koin.dsl.module

val welcomeModule = module {
    single { WelcomeNavigator() }
}
