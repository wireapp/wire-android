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

import androidx.compose.runtime.Composable
import com.wire.android.di.ViewModelScopedPreviews
import com.wire.android.di.wireManualMetroViewModelScoped
import com.wire.android.ui.common.banner.SecurityClassificationArgs
import com.wire.android.ui.common.banner.SecurityClassificationViewModel
import com.wire.android.ui.common.banner.SecurityClassificationViewModelImpl
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionsMenuViewModel
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionsMenuViewModelImpl
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory

internal interface CommonManualViewModelFactory : ManualViewModelAssistedFactory {
    fun securityClassificationViewModel(args: SecurityClassificationArgs): SecurityClassificationViewModelImpl
    fun conversationOptionsMenuViewModel(): ConversationOptionsMenuViewModelImpl
}

interface CommonViewModelGraph {
    val commonTopAppBarViewModelFactory: CommonTopAppBarViewModel.Factory
}

@Composable
fun securityClassificationViewModel(
    args: SecurityClassificationArgs,
): SecurityClassificationViewModel =
    wireManualMetroViewModelScoped<
            SecurityClassificationViewModelImpl,
            SecurityClassificationViewModel,
            SecurityClassificationArgs,
            CommonManualViewModelFactory
            >(
        arguments = args,
        previewProvider = ViewModelScopedPreviews,
    ) { _, arguments ->
        securityClassificationViewModel(arguments)
    }

@Composable
fun conversationOptionsMenuViewModel(): ConversationOptionsMenuViewModel =
    wireManualMetroViewModelScoped<
        ConversationOptionsMenuViewModelImpl,
        ConversationOptionsMenuViewModel,
        CommonManualViewModelFactory,
    >(
        previewProvider = ViewModelScopedPreviews,
    ) {
        conversationOptionsMenuViewModel()
    }
