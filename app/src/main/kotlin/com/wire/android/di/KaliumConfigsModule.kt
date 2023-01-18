package com.wire.android.di

import android.os.Build
import com.wire.android.BuildConfig
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class KaliumConfigsModule {

    @Provides
    fun provideKaliumConfigs(): KaliumConfigs {
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
            encryptProteusStorage = BuildConfig.ENCRYPT_PROTEUS_STORAGE
        )
    }


}
