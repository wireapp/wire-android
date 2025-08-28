/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations

import com.wire.android.ui.home.conversations.model.AssetBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

/**
 * WARNING:
 * This is a Singleton and is NOT lifecycle-aware.
 *
 * ⚠️ Do NOT store Android Context, Activity, View, Bitmap, InputStream, or any other
 * large or lifecycle-bound objects here, as it can lead to memory leaks.
 *
 */
class MessageSharedState @Inject constructor() {
    private val pendingBundles = MutableSharedFlow<List<AssetBundle>>()

    suspend fun postBundles(bundles: List<AssetBundle>) {
        pendingBundles.emit(bundles)
    }

    fun asFlow(): Flow<List<AssetBundle>> = pendingBundles
}
