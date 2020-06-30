package com.wire.android.core.di

import android.content.Context
import com.wire.android.core.network.di.networkModule
import com.wire.android.feature.auth.di.authenticationModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module

object Injector {

    private val coreModules: List<Module> = listOf(
        coreModule,
        networkModule
    )

    /**
     * Shared modules should contain dependencies that can
     * build up multiple features
     */
    private val sharedModules: List<Module> = listOf()

    /**
     * Feature modules should contain dependencies that build up specific
     * features and don't tend to live outside of that feature
     */
    private val featureModules: List<Module> = listOf(authenticationModules)
        .flatten()

    fun start(context: Context) {
        startKoin {
            androidContext(context)
            modules(listOf(coreModules, sharedModules, featureModules).flatten())
        }
    }
}
