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
package com.wire.android.di.metro

import com.wire.android.di.CurrentAccount
import com.wire.android.di.ImageLoadingModule
import com.wire.android.ui.MiscViewModelFactory
import com.wire.android.ui.MiscViewModelGraph
import com.wire.android.ui.authentication.AuthenticationViewModelGraph
import com.wire.android.ui.calling.CallingViewModelFactory
import com.wire.android.ui.calling.CallingViewModelGraph
import com.wire.android.ui.common.CommonViewModelFactory
import com.wire.android.ui.common.CommonViewModelGraph
import com.wire.android.ui.debug.DebugInfoViewModelFactory
import com.wire.android.ui.debug.DebugInfoViewModelGraph
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Scope
import dev.zacsweers.metro.asContribution
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

@Scope
annotation class MetroSessionScope

@GraphExtension(MetroSessionScope::class, bindingContainers = [WireMetroViewModelBindings::class, ImageLoadingModule::class])
interface AppSessionViewModelGraph :
    ViewModelGraph,
    MiscViewModelGraph,
    AuthenticationViewModelGraph,
    CallingViewModelGraph,
    DebugInfoViewModelGraph,
    CommonViewModelGraph {
    @get:CurrentAccount
    val currentAccount: UserId

    override val viewModelScopeKey: String
        get() = currentAccount.toString()

    val wireSessionImageLoader: WireSessionImageLoader

    override val miscViewModelFactory: MiscViewModelFactory
    override val callingViewModelFactory: CallingViewModelFactory
    override val debugInfoViewModelFactory: DebugInfoViewModelFactory
    override val commonViewModelFactory: CommonViewModelFactory

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    interface Factory {
        fun createAppSessionViewModelGraph(@Provides @CurrentAccount currentAccount: UserId): AppSessionViewModelGraph
    }
}

fun WireApplicationGraph.createSessionViewModelGraph(currentAccount: UserId): AppSessionViewModelGraph {
    return asContribution<AppSessionViewModelGraph.Factory>().createAppSessionViewModelGraph(currentAccount)
}
