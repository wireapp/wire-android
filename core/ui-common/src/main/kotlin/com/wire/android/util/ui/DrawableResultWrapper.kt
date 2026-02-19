/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import okio.FileSystem
import okio.Path

internal class DrawableResultWrapper {

    internal fun toFetchResult(assetPath: Path): FetchResult {
        return SourceFetchResult(
            source = ImageSource(
                file = assetPath,
                fileSystem = FileSystem.SYSTEM,
                diskCacheKey = assetPath.name,
            ),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }
}
