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
package com.wire.android.util

import com.wire.kalium.logic.data.message.AssetContent
import okio.Path
import javax.inject.Inject

/**
 * Thin wrapper around [MediaMetadata.getMediaMetadata] that allows it to be injected
 * and mocked in unit tests (the underlying implementation uses `withContext(Dispatchers.IO)`
 * which otherwise outlives the test scope).
 */
fun interface GetMediaMetadataUseCase {
    suspend operator fun invoke(filePath: Path, mimeType: String): AssetContent.AssetMetadata?
}

class GetMediaMetadataUseCaseImpl @Inject constructor() : GetMediaMetadataUseCase {
    override suspend fun invoke(filePath: Path, mimeType: String): AssetContent.AssetMetadata? =
        MediaMetadata.getMediaMetadata(filePath, mimeType)
}
