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
package com.wire.android.feature

import android.os.Process
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines whether the app is running inside a Secure Folder (Samsung) or a work profile.
 *
 * On Android, each user space is assigned a range of 100 000 UIDs.
 * Primary user space UIDs fall in the range 0–99 999 (user ID 0),
 * while secondary spaces (Secure Folder, work profiles) start at 100 000+.
 */
@Singleton
class IsSecureFolderUseCase @Inject constructor() {
    operator fun invoke(): Boolean = Process.myUid() / PER_USER_RANGE != PRIMARY_USER_ID

    companion object {
        private const val PER_USER_RANGE = 100_000
        private const val PRIMARY_USER_ID = 0
    }
}
