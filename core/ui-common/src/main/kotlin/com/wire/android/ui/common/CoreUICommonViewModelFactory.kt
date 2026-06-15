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
import com.wire.android.di.CoreUICommonViewModelScopedPreviews
import com.wire.android.di.wireManualMetroViewModelScoped
import com.wire.android.ui.common.connection.ConnectionActionButtonArgs
import com.wire.android.ui.common.connection.ConnectionActionButtonViewModel
import com.wire.android.ui.common.connection.ConnectionActionButtonViewModelImpl
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory

internal interface CoreUICommonManualViewModelFactory : ManualViewModelAssistedFactory {
    fun connectionActionButtonViewModel(args: ConnectionActionButtonArgs): ConnectionActionButtonViewModelImpl
}

@Composable
fun connectionActionButtonViewModel(
    args: ConnectionActionButtonArgs,
): ConnectionActionButtonViewModel =
    wireManualMetroViewModelScoped<
            ConnectionActionButtonViewModelImpl,
            ConnectionActionButtonViewModel,
            ConnectionActionButtonArgs,
            CoreUICommonManualViewModelFactory
            >(
        arguments = args,
        previewProvider = CoreUICommonViewModelScopedPreviews,
    ) { _, arguments ->
        connectionActionButtonViewModel(arguments)
    }

@Suppress("LongParameterList")
class CoreUICommonViewModelFactory @Inject constructor(
    private val unblockUser: UnblockUserUseCase,
    private val dispatchers: DispatcherProvider,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val cancelConnectionRequest: CancelConnectionRequestUseCase,
    private val acceptConnectionRequest: AcceptConnectionRequestUseCase,
    private val ignoreConnectionRequest: IgnoreConnectionRequestUseCase,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
) {
    internal fun connectionActionButtonViewModel(args: ConnectionActionButtonArgs) = ConnectionActionButtonViewModelImpl(
        dispatchers = dispatchers,
        sendConnectionRequest = sendConnectionRequest,
        cancelConnectionRequest = cancelConnectionRequest,
        acceptConnectionRequest = acceptConnectionRequest,
        ignoreConnectionRequest = ignoreConnectionRequest,
        unblockUser = unblockUser,
        getOrCreateOneToOneConversation = getOrCreateOneToOneConversation,
        args = args,
    )
}
