@file:Suppress("SpreadOperator")

package com.wire.android.core.di

import android.content.Context
import com.wire.android.core.network.di.networkModule
import com.wire.android.core.storage.db.di.databaseModule
import com.wire.android.feature.auth.di.authenticationModules
import com.wire.android.feature.conversation.di.conversationsModule
import com.wire.android.feature.launch.di.launcherModule
import com.wire.android.feature.welcome.di.welcomeModule
import com.wire.android.shared.session.di.sessionModule
import com.wire.android.shared.user.di.userModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module

object Injector {

    /**
     * Core modules should contain dependencies that are core
     * to the application
     */
    private val appModules: List<Module> = listOf(
        coreModule,
        accessibilityModule,
        compatibilityModule,
        appConfigModule,
        networkModule,
        databaseModule,
        uiModule
    )

    /**
     * Shared modules should contain dependencies that can
     * build up multiple features
     */
    private val sharedModules: List<Module> = listOf(userModule, sessionModule)

    /**
     * Feature modules should contain dependencies that build up specific
     * features and don't tend to live outside of that feature
     */
    private val featureModules: List<Module> = listOf(launcherModule, welcomeModule, *authenticationModules, conversationsModule)

    fun start(context: Context) {
        startKoin {
            androidContext(context)
            modules(listOf(appModules, sharedModules, featureModules).flatten())
        }
    }
}
