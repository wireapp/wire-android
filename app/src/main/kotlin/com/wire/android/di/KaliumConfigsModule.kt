/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.di

import android.content.Context
import android.os.Build
import com.wire.android.BuildConfig
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.util.isWebsocketEnabledByDefault
import com.wire.kalium.logic.featureFlags.BuildFileRestrictionState
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.HashMap

@Module
@InstallIn(SingletonComponent::class)
class KaliumConfigsModule {

    @Provides
    fun provideKaliumConfigs(globalDataStore: GlobalDataStore, context: Context): KaliumConfigs {
        val fileRestriction: BuildFileRestrictionState = if (BuildConfig.FILE_RESTRICTION_ENABLED) {
            BuildConfig.FILE_RESTRICTION_LIST.split(",").map { it.trim() }.let {
                BuildFileRestrictionState.AllowSome(it)
            }
        } else {
            BuildFileRestrictionState.NoRestriction
        }

        return object : KaliumConfigs() {

            override val fileRestrictionState = fileRestriction

            override val forceConstantBitrateCalls = BuildConfig.FORCE_CONSTANT_BITRATE_CALLS

            // we use upsert, available from SQL3.24, which is supported from Android API30, so for older APIs we have to use SQLCipher
            override val shouldEncryptData = !BuildConfig.DEBUG || Build.VERSION.SDK_INT < Build.VERSION_CODES.R
            override val lowerKeyPackageLimits = BuildConfig.PRIVATE_BUILD
            override val lowerKeyingMaterialsUpdateThreshold = BuildConfig.PRIVATE_BUILD
            override var isMLSSupportEnabled = BuildConfig.MLS_SUPPORT_ENABLED
            override val developmentApiEnabled = BuildConfig.DEVELOPMENT_API_ENABLED
            override val encryptProteusStorage = runBlocking { globalDataStore.isEncryptedProteusStorageEnabled().first() }
            override val guestRoomLink = BuildConfig.ENABLE_GUEST_ROOM_LINK
            override val selfDeletingMessages = BuildConfig.SELF_DELETING_MESSAGES
            override val wipeOnCookieInvalid = BuildConfig.WIPE_ON_COOKIE_INVALID
            override val wipeOnDeviceRemoval = BuildConfig.WIPE_ON_DEVICE_REMOVAL
            override val wipeOnRootedDevice = BuildConfig.WIPE_ON_ROOTED_DEVICE
            override val isWebSocketEnabledByDefault = isWebsocketEnabledByDefault(context)
            override fun certPinningConfig(): Map<String, List<String>> = BuildConfig.CERTIFICATE_PINNING_CONFIG
        }
    }
}
