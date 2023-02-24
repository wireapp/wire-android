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

import android.os.Build
import com.wire.android.BuildConfig
import com.wire.android.datastore.GlobalDataStore
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
class KaliumConfigsModule {

    @Provides
    fun provideKaliumConfigs(globalDataStore: GlobalDataStore): KaliumConfigs {
        return KaliumConfigs(
            isChangeEmailEnabled = BuildConfig.ALLOW_CHANGE_OF_EMAIL,
            isLoggingEnabled = BuildConfig.LOGGING_ENABLED,
            blacklistHost = BuildConfig.BLACKLIST_HOST,
            fileRestrictionEnabled = BuildConfig.FILE_RESTRICTION_ENABLED,
            forceConstantBitrateCalls = BuildConfig.FORCE_CONSTANT_BITRATE_CALLS,
            domain = BuildConfig.DOMAIN,
            developerFeaturesEnabled = BuildConfig.DEVELOPER_FEATURES_ENABLED,
            isSafeLoggingEnabled = BuildConfig.SAFE_LOGGING,
            enableBlacklist = BuildConfig.ENABLE_BLACK_LIST,
            certificate = BuildConfig.CERTIFICATE,
            maxAccount = BuildConfig.MAX_ACCOUNTS,
            // we use upsert, available from SQL3.24, which is supported from Android API30, so for older APIs we have to use SQLCipher
            shouldEncryptData = !BuildConfig.DEBUG || Build.VERSION.SDK_INT < Build.VERSION_CODES.R,
            lowerKeyPackageLimits = BuildConfig.PRIVATE_BUILD,
            lowerKeyingMaterialsUpdateThreshold = BuildConfig.PRIVATE_BUILD,
            isMLSSupportEnabled = BuildConfig.MLS_SUPPORT_ENABLED,
            developmentApiEnabled = BuildConfig.DEVELOPMENT_API_ENABLED,
            encryptProteusStorage = runBlocking { globalDataStore.isEncryptedProteusStorageEnabled().first() }
        )
    }
}
