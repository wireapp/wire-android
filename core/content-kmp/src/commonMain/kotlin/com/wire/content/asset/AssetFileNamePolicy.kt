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

package com.wire.content.asset

object AssetFileNamePolicy {
    fun isCompatible(fileName: String): Boolean =
        fileName != "." &&
            !fileName.startsWith('.') &&
            !fileName.contains('/') &&
            !fileName.contains('\\') &&
            !fileName.contains('"')

    fun sanitize(fileName: String): String = fileName
        .trimStart('.')
        .replace("/", "_")
        .replace("\\", "_")
        .replace("\"", "_")
        .ifEmpty { "file" }
}
