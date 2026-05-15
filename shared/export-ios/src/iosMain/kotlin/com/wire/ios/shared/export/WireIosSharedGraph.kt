/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.ios.shared.export

import com.wire.ios.shared.IosKaliumRuntimeConfig
import com.wire.ios.shared.IosViewModel
import com.wire.ios.shared.MigrationMode
import com.wire.ios.shared.WireIosSharedConfig
import com.wire.ios.shared.WireIosSharedScope
import com.wire.ios.shared.auth.email.KaliumLoginEmailBackend
import com.wire.ios.shared.auth.email.LoginEmailBackend
import com.wire.ios.shared.auth.email.LoginEmailEffect
import com.wire.ios.shared.auth.email.LoginEmailIntent
import com.wire.ios.shared.auth.email.LoginEmailIosViewModel
import com.wire.ios.shared.auth.email.LoginEmailIosViewModelFactory
import com.wire.ios.shared.auth.email.LoginEmailState
import com.wire.ios.shared.auth.email.createGenericLoginEmailIosViewModel
import com.wire.ios.shared.auth.email.createLoginEmailIosViewModel
import com.wire.ios.shared.auth.login.model.LoginServerLinks
import com.wire.ios.shared.auth.newlogin.KaliumNewLoginIdentifierBackend
import com.wire.ios.shared.auth.newlogin.NewLoginIdentifierBackend
import com.wire.ios.shared.auth.newlogin.NewLoginIdentifierEffect
import com.wire.ios.shared.auth.newlogin.NewLoginIdentifierIntent
import com.wire.ios.shared.auth.newlogin.NewLoginIdentifierIosViewModel
import com.wire.ios.shared.auth.newlogin.NewLoginIdentifierIosViewModelFactory
import com.wire.ios.shared.auth.newlogin.NewLoginIdentifierState
import com.wire.ios.shared.auth.newlogin.createGenericNewLoginIdentifierIosViewModel
import com.wire.ios.shared.auth.newlogin.createNewLoginIdentifierIosViewModel
import com.wire.ios.shared.auth.welcome.WelcomeEffect
import com.wire.ios.shared.auth.welcome.WelcomeIntent
import com.wire.ios.shared.auth.welcome.WelcomeIosViewModel
import com.wire.ios.shared.auth.welcome.WelcomeIosViewModelFactory
import com.wire.ios.shared.auth.welcome.WelcomeState
import com.wire.ios.shared.auth.welcome.createGenericWelcomeIosViewModel
import com.wire.ios.shared.auth.welcome.createWelcomeIosViewModel
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.CoreLogicCommon
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory

@DependencyGraph(WireIosSharedScope::class)
interface WireIosSharedGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides config: WireIosSharedConfig): WireIosSharedGraph
    }

    @SingleIn(WireIosSharedScope::class)
    @Provides
    fun provideCoreLogic(config: WireIosSharedConfig): CoreLogicCommon {
        val runtime = requireNotNull(config.runtimeConfig) {
            "IosKaliumRuntimeConfig is required for Kalium-backed iOS login probes."
        }
        require(runtime.migrationMode == MigrationMode.CleanInstallProbe) {
            "Only CleanInstallProbe is supported by the current iOS Kalium login probe."
        }
        return CoreLogic(
            rootPath = runtime.sqlDelightRootPath,
            kaliumConfigs = KaliumConfigs(),
            userAgent = "WireIosShared/1.0 iOS",
        )
    }

    @Provides
    fun provideWelcomeIosViewModel(
        welcomeIosViewModelFactory: WelcomeIosViewModelFactory,
    ): WelcomeIosViewModel =
        createWelcomeIosViewModel(welcomeIosViewModelFactory)

    @Provides
    fun provideGenericWelcomeIosViewModel(
        welcomeIosViewModelFactory: WelcomeIosViewModelFactory,
    ): IosViewModel<WelcomeState, WelcomeEffect, WelcomeIntent> =
        createGenericWelcomeIosViewModel(welcomeIosViewModelFactory)

    @Provides
    fun provideNewLoginIdentifierViewModel(
        newLoginIdentifierIosViewModelFactory: NewLoginIdentifierIosViewModelFactory,
    ): NewLoginIdentifierIosViewModel =
        createNewLoginIdentifierIosViewModel(newLoginIdentifierIosViewModelFactory)

    @Provides
    fun provideGenericNewLoginIdentifierIosViewModel(
        newLoginIdentifierIosViewModelFactory: NewLoginIdentifierIosViewModelFactory,
    ): IosViewModel<NewLoginIdentifierState, NewLoginIdentifierEffect, NewLoginIdentifierIntent> =
        createGenericNewLoginIdentifierIosViewModel(newLoginIdentifierIosViewModelFactory)

    @Provides
    fun provideNewLoginIdentifierBackend(
        backend: KaliumNewLoginIdentifierBackend,
    ): NewLoginIdentifierBackend = backend

    @Provides
    fun provideLoginEmailViewModel(
        loginEmailIosViewModelFactory: LoginEmailIosViewModelFactory,
    ): LoginEmailIosViewModel =
        createLoginEmailIosViewModel(loginEmailIosViewModelFactory)

    @Provides
    fun provideGenericLoginEmailIosViewModel(
        loginEmailIosViewModelFactory: LoginEmailIosViewModelFactory,
    ): IosViewModel<LoginEmailState, LoginEmailEffect, LoginEmailIntent> =
        createGenericLoginEmailIosViewModel(loginEmailIosViewModelFactory)

    @Provides
    fun provideLoginEmailBackend(
        backend: KaliumLoginEmailBackend,
    ): LoginEmailBackend = backend

    val welcomeViewModel: WelcomeIosViewModel
    val welcomeIosViewModel: IosViewModel<WelcomeState, WelcomeEffect, WelcomeIntent>
    val newLoginIdentifierViewModel: NewLoginIdentifierIosViewModel
    val newLoginIdentifierIosViewModel: IosViewModel<NewLoginIdentifierState, NewLoginIdentifierEffect, NewLoginIdentifierIntent>
    val loginEmailViewModel: LoginEmailIosViewModel
    val loginEmailIosViewModel: IosViewModel<LoginEmailState, LoginEmailEffect, LoginEmailIntent>

    /**
     * Releases resources owned by the export graph.
     *
     * Kalium's CoreLogic currently does not expose a public close hook for its global/session
     * providers. ViewModel coroutine scopes are released by each concrete ViewModel's close().
     * iOS should keep this graph alive for the app or debug-probe lifetime instead of creating one
     * graph per SwiftUI render or per screen.
     */
    fun close() = Unit
}

fun createWireIosSharedGraph(config: WireIosSharedConfig): WireIosSharedGraph =
    createGraphFactory<WireIosSharedGraph.Factory>().create(config)

fun createWireIosShared(defaultServerLinks: LoginServerLinks): WireIosSharedGraph =
    createWireIosSharedGraph(
        config = com.wire.ios.shared.createWireIosSharedConfig(defaultServerLinks)
    )

fun createWireIosSharedProbe(
    defaultServerLinks: LoginServerLinks,
    runtimeConfig: IosKaliumRuntimeConfig? = null,
): WireIosSharedGraph =
    createWireIosSharedGraph(
        config = com.wire.ios.shared.createWireIosSharedConfig(
            defaultServerLinks = defaultServerLinks,
            runtimeConfig = runtimeConfig,
        )
    )
