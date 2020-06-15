package com.wire.android.feature.auth.registration.di

import com.wire.android.feature.auth.registration.personal.email.CreatePersonalAccountEmailViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val registrationModule: Module = module {
    viewModel { CreatePersonalAccountEmailViewModel() }
}