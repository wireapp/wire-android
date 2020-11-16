package com.wire.android.feature.launch.di

import com.wire.android.feature.launch.ui.LauncherViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val launcherModule = module {
    viewModel { LauncherViewModel(get()) }
}
