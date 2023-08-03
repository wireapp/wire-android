/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.util

import android.content.Context
import com.wire.android.BuildConfig
import com.wire.android.util.extension.isGoogleServicesAvailable

/**
 * If [BuildConfig.WEBSOCKET_ENABLED_BY_DEFAULT] is true, the websocket should be enabled by default always.
 * Otherwise, it should be enabled by default only if Google Play Services are not available.
 */
fun isWebsocketEnabledByDefault(context: Context) =
    BuildConfig.WEBSOCKET_ENABLED_BY_DEFAULT || !context.isGoogleServicesAvailable()
