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

package com.wire.android.nomaddevice.initializer

import android.content.Context
import androidx.startup.Initializer
import com.wire.android.nomaddevice.dao.NomadDeviceDaoRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

class NomadDeviceInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val appContext = context.applicationContext ?: return
        val entryPoint = EntryPointAccessors.fromApplication(appContext, NomadDeviceInitializerEntryPoint::class.java)
        runBlocking {
            entryPoint.nomadDeviceDaoRepository().warmUp()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NomadDeviceInitializerEntryPoint {
    fun nomadDeviceDaoRepository(): NomadDeviceDaoRepository
}
