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

import android.content.Context
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryViewModelFactory
import com.wire.android.ui.home.settings.about.dependencies.AndroidDependenciesInfoProvider
import com.wire.android.ui.home.settings.about.dependencies.DependenciesInfoProvider
import com.wire.android.ui.home.settings.about.dependencies.DependenciesViewModelFactory
import com.wire.android.ui.home.settings.about.licenses.AndroidLicensesProvider
import com.wire.android.ui.home.settings.about.licenses.LicensesProvider
import com.wire.android.ui.home.settings.about.licenses.LicensesViewModelFactory
import com.wire.android.ui.home.whatsnew.AndroidReleaseNotesFeedUrlProvider
import com.wire.android.ui.home.whatsnew.ReleaseNotesFeedUrlProvider
import com.wire.android.ui.home.whatsnew.WhatsNewViewModelFactory
import com.wire.android.ui.settings.about.AboutThisAppInfoProvider
import com.wire.android.ui.settings.about.AboutThisAppViewModelFactory
import com.wire.android.ui.settings.about.AndroidAboutThisAppInfoProvider
import com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModelFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

abstract class WireMetroScope private constructor()

@DependencyGraph(WireMetroScope::class)
interface WireMetroGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides @ApplicationContext context: Context): WireMetroGraph
    }

    val checkAssetRestrictionsViewModelFactory: CheckAssetRestrictionsViewModelFactory
    val aboutThisAppViewModelFactory: AboutThisAppViewModelFactory
    val createAccountSummaryViewModelFactory: CreateAccountSummaryViewModelFactory
    val whatsNewViewModelFactory: WhatsNewViewModelFactory
    val dependenciesViewModelFactory: DependenciesViewModelFactory
    val licensesViewModelFactory: LicensesViewModelFactory

    @Provides
    fun provideAboutThisAppInfoProvider(
        @ApplicationContext context: Context,
    ): AboutThisAppInfoProvider = AndroidAboutThisAppInfoProvider(context)

    @Provides
    fun provideReleaseNotesFeedUrlProvider(
        @ApplicationContext context: Context,
    ): ReleaseNotesFeedUrlProvider = AndroidReleaseNotesFeedUrlProvider(context)

    @Provides
    fun provideDependenciesInfoProvider(
        @ApplicationContext context: Context,
    ): DependenciesInfoProvider = AndroidDependenciesInfoProvider(context)

    @Provides
    fun provideLicensesProvider(
        @ApplicationContext context: Context,
    ): LicensesProvider = AndroidLicensesProvider(context)
}

fun createWireMetroGraph(context: Context): WireMetroGraph =
    createGraphFactory<WireMetroGraph.Factory>().create(context.applicationContext)
