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

package com.wire.android.util.ui

import coil3.ImageLoader
import com.wire.kalium.network.NetworkStateObserver
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class WireSessionImageLoaderTest {

    @Test
    fun givenSessionImageLoader_whenShutdown_thenPendingImageRequestsAreCancelled() {
        val coilImageLoader = mockk<ImageLoader>(relaxed = true)
        val imageLoader = WireSessionImageLoader(
            coilImageLoader = coilImageLoader,
            networkStateObserver = mockk<NetworkStateObserver>(relaxed = true)
        )

        imageLoader.shutdown()

        verify(exactly = 1) { coilImageLoader.shutdown() }
    }
}
