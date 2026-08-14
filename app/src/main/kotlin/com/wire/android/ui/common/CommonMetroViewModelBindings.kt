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
package com.wire.android.ui.common

import androidx.lifecycle.ViewModel
import com.wire.android.ui.common.banner.SecurityClassificationArgs
import com.wire.android.ui.common.banner.SecurityClassificationViewModelImpl
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionsMenuViewModelImpl
import com.wire.android.ui.common.topappbar.CommonTopAppBarParams
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
object CommonMetroViewModelBindings {

    @Provides
    @IntoMap
    @ViewModelKey(ConversationOptionsMenuViewModelImpl::class)
    fun conversationOptionsMenuViewModel(viewModel: ConversationOptionsMenuViewModelImpl): ViewModel = viewModel

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(CommonManualViewModelFactory::class)
    internal fun commonManualViewModelFactory(
        commonTopAppBarFactory: CommonTopAppBarViewModel.Factory,
        securityClassificationFactory: SecurityClassificationViewModelImpl.Factory,
    ): ManualViewModelAssistedFactory = object : CommonManualViewModelFactory {
        override fun commonTopAppBarViewModel(params: CommonTopAppBarParams): CommonTopAppBarViewModel =
            commonTopAppBarFactory.create(params)

        override fun securityClassificationViewModel(args: SecurityClassificationArgs): SecurityClassificationViewModelImpl =
            securityClassificationFactory.create(args)
    }
}
