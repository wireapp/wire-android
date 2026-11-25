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

import com.wire.android.di.metro.MetroSessionScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.isActive

/**
 * The node lists of the Drive and Shared Drive listings currently open, by the path of the folder they show.
 *
 * The search screen starts from the unfiltered list of the listing it was opened from, which stays open below it: it
 * shares that list rather than loading it again from the server.
 */
@SingleIn(MetroSessionScope::class)
class CellNodeListRegistry @Inject constructor() {

    private val lists = mutableMapOf<FolderKey, CellNodeList>()

    @Synchronized
    internal fun register(path: String?, list: CellNodeList) {
        lists[FolderKey(path)] = list
    }

    /** Only if [list] is still the one registered for [path]: another listing of the same folder may have replaced it. */
    @Synchronized
    internal fun unregister(path: String?, list: CellNodeList) {
        val key = FolderKey(path)
        if (lists[key] === list) lists.remove(key)
    }

    /** The list of the listing open at [path], if any and still alive. */
    @Synchronized
    internal fun find(path: String?): CellNodeList? = lists[FolderKey(path)]?.takeIf { it.scope.isActive }

    // The path is null for the Drive (All Files) listing.
    private data class FolderKey(val path: String?)
}
