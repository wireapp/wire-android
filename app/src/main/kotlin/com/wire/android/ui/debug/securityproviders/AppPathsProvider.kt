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
package com.wire.android.ui.debug.securityproviders

import android.content.Context
import com.wire.android.R
import com.wire.android.di.ApplicationContext
import com.wire.android.di.CurrentAccount
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metro.Inject

class AppPathsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @CurrentAccount private val currentAccount: UserId,
) {
    operator fun invoke(): List<LabelledValue> = with(context) {
        val accountSuffix = "${currentAccount.domain}/${currentAccount.value}"
        listOf(
            LabelledValue(R.string.debug_settings_app_path_assets, "$filesDir/$accountSuffix"),
            LabelledValue(R.string.debug_settings_app_path_cache, "$cacheDir/$accountSuffix"),
            LabelledValue(R.string.debug_settings_app_path_files_dir, filesDir.absolutePath),
            LabelledValue(R.string.debug_settings_app_path_cache_dir, cacheDir.absolutePath),
            LabelledValue(R.string.debug_settings_app_path_databases_dir, getDatabasePath(DATABASE_NAME_PROBE).parent.orEmpty()),
            LabelledValue(R.string.debug_settings_app_path_no_backup_dir, noBackupFilesDir.absolutePath),
            LabelledValue(R.string.debug_settings_app_path_external_files_dir, getExternalFilesDir(null)?.absolutePath.orEmpty()),
        )
    }

    private companion object {
        /** Only used to resolve the databases directory, the file itself never has to exist. */
        const val DATABASE_NAME_PROBE = "probe"
    }
}
