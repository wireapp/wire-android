package com.wire.android.core.di

import android.content.Context
import com.wire.android.feature.auth.registration.di.registrationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module

object Injector {

    private val coreModules: List<Module> = listOf()

    /**
     * Shared modules should contain dependencies that can
     * build up multiple features
     */
    private val sharedModules: List<Module> = listOf()

    /**
     * Feature modules should contain dependencies that build up specific
     * features and don't tend to live outside of that feature
     */
    private val featureModules: List<Module> = listOf(registrationModule)

    fun start(context: Context) {
        startKoin {
            androidContext(context)
            modules(listOf(coreModules, sharedModules, featureModules).flatten())
        }
    }
}
