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
@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui.common

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wire.android.di.ViewModelScopedPreviews
import com.wire.android.di.metro.wireAssistedMetroViewModel
import com.wire.android.di.metro.wireAssistedMetroViewModelAs
import com.wire.android.di.metro.wireMetroViewModelAs
import com.wire.android.ui.common.banner.SecurityClassificationArgs
import com.wire.android.ui.common.banner.SecurityClassificationViewModel
import com.wire.android.ui.common.banner.SecurityClassificationViewModelImpl
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionsMenuViewModel
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionsMenuViewModelImpl
import com.wire.android.ui.common.topappbar.CommonTopAppBarParams
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory

internal interface CommonManualViewModelFactory : ManualViewModelAssistedFactory {
    fun commonTopAppBarViewModel(params: CommonTopAppBarParams): CommonTopAppBarViewModel
    fun securityClassificationViewModel(args: SecurityClassificationArgs): SecurityClassificationViewModelImpl
}

@Composable
fun commonTopAppBarViewModel(
    params: CommonTopAppBarParams,
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
): CommonTopAppBarViewModel =
    wireAssistedMetroViewModel<CommonTopAppBarViewModel, CommonManualViewModelFactory>(owner = owner) { _ ->
        commonTopAppBarViewModel(params)
    }

@Composable
fun securityClassificationViewModel(
    args: SecurityClassificationArgs,
): SecurityClassificationViewModel =
    wireAssistedMetroViewModelAs<
        SecurityClassificationViewModelImpl,
        SecurityClassificationViewModel,
        CommonManualViewModelFactory,
        >(
        instanceKey = args.key,
        previewProvider = ViewModelScopedPreviews,
    ) { _ ->
        securityClassificationViewModel(args)
    }

@Composable
fun conversationOptionsMenuViewModel(): ConversationOptionsMenuViewModel =
    wireMetroViewModelAs<ConversationOptionsMenuViewModelImpl, ConversationOptionsMenuViewModel>(
        previewProvider = ViewModelScopedPreviews,
    )
