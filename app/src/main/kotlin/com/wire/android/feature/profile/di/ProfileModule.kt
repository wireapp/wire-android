package com.wire.android.feature.profile.di

import com.wire.android.R
import com.wire.android.core.ui.navigation.FragmentContainerProvider
import com.wire.android.feature.profile.ui.ProfileActivity
import com.wire.android.feature.profile.ui.ProfileNavigator
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

val profileModule = module {
    single { ProfileNavigator() }
    factory(qualifier<ProfileActivity>()) {
        FragmentContainerProvider.fixedProvider(R.id.profileSettingsFragmentContainer)
    }
}
