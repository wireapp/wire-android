package com.wire.android.core.di

import com.wire.android.core.usecase.executors.ObservableUseCaseExecutor
import com.wire.android.core.usecase.executors.OneShotUseCaseExecutor
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule: Module = module {
    factory { ObservableUseCaseExecutor() }
    factory { OneShotUseCaseExecutor() }
}