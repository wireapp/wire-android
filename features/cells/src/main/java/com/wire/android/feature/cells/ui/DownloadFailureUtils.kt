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
package com.wire.android.feature.cells.ui

import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.common.error.StorageFailure
import java.io.IOException

/**
 * Returns true when this [CoreFailure] represents a "no space left on device" condition.
 *
 * Detection strategy:
 * 1. [StorageFailure.Generic] — Kalium wraps file-write IOExceptions here in some paths.
 * 2. [NetworkFailure.ServerMiscommunication] — CellsDataSource.downloadFile catches all
 *    Exceptions (including ENOSPC IOExceptions from okio sink writes) and wraps them as
 *    ServerMiscommunication. We traverse rootCause to detect the underlying IOException.
 * 3. Full cause-chain walk so any wrapping layer doesn't hide the ENOSPC signal.
 * 4. Match against multiple OS-level ENOSPC message variants (POSIX, Android, Windows-like).
 */
internal fun CoreFailure.isNoSpaceLeft(): Boolean = when (this) {
    is StorageFailure.Generic -> rootCause.causedByNoSpace()
    is NetworkFailure.ServerMiscommunication -> rootCause.causedByNoSpace()
    else -> false
}

/**
 * Walks the full [Throwable] cause chain searching for an [IOException] whose message
 * indicates a full-disk condition.
 */
private fun Throwable.causedByNoSpace(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is IOException && current.message.isNoSpaceMessage()) return true
        current = current.cause
    }
    return false
}

private fun String?.isNoSpaceMessage(): Boolean {
    if (this == null) return false
    return contains("ENOSPC", ignoreCase = true) ||
        contains("no space left", ignoreCase = true) ||
        contains("not enough space", ignoreCase = true) ||
        contains("device is full", ignoreCase = true) ||
        contains("disk full", ignoreCase = true)
}

