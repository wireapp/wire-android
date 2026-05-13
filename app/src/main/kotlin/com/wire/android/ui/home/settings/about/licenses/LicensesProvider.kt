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
package com.wire.android.ui.home.settings.about.licenses

import android.content.Context
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.util.withContext
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface LicensesProvider {
    suspend fun getLibraries(): List<Library>
}

class AndroidLicensesProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LicensesProvider {

    override suspend fun getLibraries(): List<Library> = withContext(Dispatchers.IO) {
        Libs.Builder()
            .withContext(context)
            .build()
            .libraries
            .distinctBy { it.uniqueId }
    }
}

@Module
@InstallIn(ViewModelComponent::class)
interface LicensesProviderModule {

    @Binds
    fun bindLicensesProvider(
        provider: AndroidLicensesProvider
    ): LicensesProvider
}
