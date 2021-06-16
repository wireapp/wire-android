@file:Suppress("SpreadOperator")

package com.wire.android.core.di

import android.content.Context
import com.wire.android.core.network.di.networkModule
import com.wire.android.core.storage.db.di.databaseModule
import com.wire.android.core.websocket.di.webSocketModule
import com.wire.android.feature.auth.di.authenticationModules
import com.wire.android.feature.contact.di.contactModule
import com.wire.android.feature.conversation.di.conversationModules
import com.wire.android.feature.launch.di.launcherModule
import com.wire.android.feature.profile.di.profileModule
import com.wire.android.feature.sync.di.syncModule
import com.wire.android.feature.welcome.di.welcomeModule
import com.wire.android.shared.asset.di.assetModule
import com.wire.android.shared.session.di.sessionModule
import com.wire.android.shared.team.di.teamModule
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
        asyncModule,
        networkModule,
        databaseModule,
        uiModule,
        ioModule,
        cryptoBoxModule,
        webSocketModule
    )

    /**
     * Shared modules should contain dependencies that can
     * build up multiple features
     */
    private val sharedModules: List<Module> = listOf(userModule, sessionModule, teamModule, assetModule)

    /**
     * Feature modules should contain dependencies that build up specific
     * features and don't tend to live outside of that feature
     */
    private val featureModules: List<Module> =
        listOf(launcherModule, welcomeModule, *authenticationModules, syncModule, *conversationModules, contactModule, profileModule)

    fun start(context: Context) {
        startKoin {
            androidContext(context)
            modules(listOf(appModules, sharedModules, featureModules).flatten())
        }
    }
}
