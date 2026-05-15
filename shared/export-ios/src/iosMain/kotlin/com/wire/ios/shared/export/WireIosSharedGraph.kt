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

import com.wire.ios.shared.IosViewModel
import com.wire.ios.shared.WireIosSharedConfig
import com.wire.ios.shared.WireIosSharedScope
import com.wire.ios.shared.auth.login.model.LoginServerLinks
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
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

@DependencyGraph(WireIosSharedScope::class)
interface WireIosSharedGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides config: WireIosSharedConfig): WireIosSharedGraph
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

    val welcomeViewModel: WelcomeIosViewModel
    val welcomeIosViewModel: IosViewModel<WelcomeState, WelcomeEffect, WelcomeIntent>
    val newLoginIdentifierViewModel: NewLoginIdentifierIosViewModel
    val newLoginIdentifierIosViewModel: IosViewModel<NewLoginIdentifierState, NewLoginIdentifierEffect, NewLoginIdentifierIntent>
}

fun createWireIosSharedGraph(config: WireIosSharedConfig): WireIosSharedGraph =
    createGraphFactory<WireIosSharedGraph.Factory>().create(config)

fun createWireIosShared(defaultServerLinks: LoginServerLinks): WireIosSharedGraph =
    createWireIosSharedGraph(
        config = com.wire.ios.shared.createWireIosSharedConfig(defaultServerLinks)
    )
