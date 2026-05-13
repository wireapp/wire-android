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
package com.wire.android.ui

import android.content.Intent
import androidx.core.net.toUri
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.lifecycle.AutomatedLoginViaSSO
import com.wire.android.util.lifecycle.IntentsProcessor
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import javax.inject.Inject

data class WireActivityIntentContent(
    val dataUri: String?,
    val action: String?,
    val automatedLogin: String?,
)

interface WireActivityIntentGateway {
    suspend fun parseDeepLink(intentContent: WireActivityIntentContent?): DeepLinkResult
    suspend fun parseAutomatedLogin(intentContent: WireActivityIntentContent?): AutomatedLoginViaSSO?
}

class AndroidWireActivityIntentGateway @Inject constructor(
    private val deepLinkProcessor: Lazy<DeepLinkProcessor>,
    private val intentsProcessor: Lazy<IntentsProcessor>,
) : WireActivityIntentGateway {

    override suspend fun parseDeepLink(intentContent: WireActivityIntentContent?): DeepLinkResult =
        deepLinkProcessor.get().invoke(intentContent?.dataUri?.toUri(), intentContent?.action)

    override suspend fun parseAutomatedLogin(intentContent: WireActivityIntentContent?): AutomatedLoginViaSSO? =
        intentsProcessor.get().parseAutomatedLogin(intentContent?.automatedLogin)
}

fun Intent.toWireActivityIntentContent(): WireActivityIntentContent =
    WireActivityIntentContent(
        dataUri = data?.toString(),
        action = action,
        automatedLogin = getStringExtra(IntentsProcessor.AUTOMATED_LOGIN),
    )

@Module
@InstallIn(ViewModelComponent::class)
interface WireActivityIntentGatewayModule {
    @Binds
    fun bindWireActivityIntentGateway(gateway: AndroidWireActivityIntentGateway): WireActivityIntentGateway
}
