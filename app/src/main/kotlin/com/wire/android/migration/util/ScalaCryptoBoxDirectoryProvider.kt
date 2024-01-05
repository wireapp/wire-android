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

package com.wire.android.migration.util

import android.content.Context
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScalaCryptoBoxDirectoryProvider @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) {
    fun rootDir(): File = File(applicationContext.filesDir, SCALA_CRYPTO_BOX_DIR_NAME)
    fun userDir(userId: UserId): File = File(rootDir(), userId.value)

    companion object {
        private const val SCALA_CRYPTO_BOX_DIR_NAME = "otr"
    }
}
