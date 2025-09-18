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
package com.wire.android.emm

import android.content.Context
import android.content.RestrictionsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagedConfigurationsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val restrictionsManager: RestrictionsManager by lazy {
        context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
    }

    fun getBooleanRestrictionByKey(key: String, defaultValue: Boolean = false): Boolean {
        return restrictionsManager.applicationRestrictions.getBoolean(key, defaultValue)
    }

    fun getStringRestrictionByKey(key: String, defaultValue: String? = null): String? {
        return restrictionsManager.applicationRestrictions.getString(key) ?: defaultValue
    }

    companion object {
        const val TAG = "ManagedConfigurationsRepository"
    }
}
