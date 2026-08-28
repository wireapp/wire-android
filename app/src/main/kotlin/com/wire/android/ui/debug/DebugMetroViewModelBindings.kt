/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.debug

import androidx.lifecycle.ViewModel
import com.wire.android.ui.debug.cryptostats.ConversationCryptoStatsViewModel
import com.wire.android.ui.debug.featureflags.DebugFeatureFlagsViewModel
import com.wire.android.ui.debug.securityproviders.SecurityProvidersViewModel
import com.wire.android.ui.home.settings.about.dependencies.DependenciesViewModel
import com.wire.android.ui.home.settings.about.licenses.LicensesViewModel
import com.wire.android.ui.home.whatsnew.WhatsNewViewModel
import com.wire.android.ui.settings.about.AboutThisAppViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
object DebugMetroViewModelBindings {

    @Provides @IntoMap @ViewModelKey(UserDebugViewModel::class)
    fun userDebugViewModel(viewModel: UserDebugViewModel): ViewModel = viewModel

    @Provides @IntoMap @ViewModelKey(LogManagementViewModel::class)
    fun logManagementViewModel(viewModel: LogManagementViewModel): ViewModel = viewModel

    @Provides @IntoMap @ViewModelKey(DebugDataOptionsViewModelImpl::class)
    fun debugDataOptionsViewModel(viewModel: DebugDataOptionsViewModelImpl): ViewModel = viewModel

    @Provides @IntoMap @ViewModelKey(ExportObfuscatedCopyViewModelImpl::class)
    fun exportObfuscatedCopyViewModel(viewModel: ExportObfuscatedCopyViewModelImpl): ViewModel = viewModel

    @Provides @IntoMap @ViewModelKey(ConversationCryptoStatsViewModel::class)
    fun conversationCryptoStatsViewModel(viewModel: ConversationCryptoStatsViewModel): ViewModel = viewModel

    @Provides @IntoMap @ViewModelKey(DebugFeatureFlagsViewModel::class)
    fun debugFeatureFlagsViewModel(viewModel: DebugFeatureFlagsViewModel): ViewModel = viewModel

    @Provides @IntoMap @ViewModelKey(SecurityProvidersViewModel::class)
    fun securityProvidersViewModel(viewModel: SecurityProvidersViewModel): ViewModel = viewModel

    @Provides @IntoMap @ViewModelKey(WhatsNewViewModel::class)
    fun whatsNewViewModel(viewModel: WhatsNewViewModel): ViewModel = viewModel

    @Provides @IntoMap @ViewModelKey(AboutThisAppViewModel::class)
    fun aboutThisAppViewModel(viewModel: AboutThisAppViewModel): ViewModel = viewModel

    @Provides @IntoMap @ViewModelKey(DependenciesViewModel::class)
    fun dependenciesViewModel(viewModel: DependenciesViewModel): ViewModel = viewModel

    @Provides @IntoMap @ViewModelKey(LicensesViewModel::class)
    fun licensesViewModel(viewModel: LicensesViewModel): ViewModel = viewModel
}
