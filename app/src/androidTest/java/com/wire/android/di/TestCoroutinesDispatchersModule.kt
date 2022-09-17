package com.wire.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.ExperimentalCoroutinesApi

@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoroutinesDispatchersModule::class]
)

@OptIn(ExperimentalCoroutinesApi::class)
@Module
object TestCoroutinesDispatchersModule {

    @DefaultDispatcher
    @Provides
    fun providesDefaultDispatcher(): CoroutineDispatcher =
        StandardTestDispatcher()

    @IoDispatcher
    @Provides
    fun providesIoDispatcher(): CoroutineDispatcher =
        StandardTestDispatcher()

    @MainDispatcher
    @Provides
    fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
