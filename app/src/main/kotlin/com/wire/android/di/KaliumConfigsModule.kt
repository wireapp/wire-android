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
            isAccountCreationEnabled = BuildConfig.ALLOW_ACCOUNT_CREATION,
            isChangeEmailEnabled = BuildConfig.ALLOW_CHANGE_OF_EMAIL,
            isLoggingEnabled = BuildConfig.LOGGING_ENABLED,
            isSSoEnabled = BuildConfig.ALLOW_SSO,
            blockOnJailbreakOrRoot = BuildConfig.BLOCK_ON_JAILBREAK_OR_ROOT,
            blacklistHost = BuildConfig.BLACKLIST_HOST,
            blockOnPasswordPolicy = BuildConfig.BLOCK_ON_PASSWORD_POLICY,
            fileRestrictionEnabled = BuildConfig.FILE_RESTRICTION_ENABLED,
            fileRestrictionList = BuildConfig.FILE_RESTRICTION_LIST,
            forceConstantBitrateCalls = BuildConfig.FORCE_CONSTANT_BITRATE_CALLS,
            keepWebSocketOn = BuildConfig.KEEP_WEB_SOCKET_ON,
            webLinkPreview = BuildConfig.WEB_LINK_PREVIEW,
            websiteUrl = BuildConfig.WEBSITE_URL,
            webSocketUrl = BuildConfig.WEB_SOCKET_URL,
            wipeOnCookieInvalid = BuildConfig.WIPE_ON_COOKIE_INVALID,
            submitCrashReports = BuildConfig.SUBMIT_CRASH_REPORTS,
            domain = BuildConfig.DOMAIN,
            developerFeaturesEnabled = BuildConfig.DEVELOPER_FEATURES_ENABLED,
            isSafeLoggingEnabled = BuildConfig.SAFE_LOGGING,
            enableBlacklist = BuildConfig.ENABLE_BLACK_LIST,
            certificate = BuildConfig.CERTIFICATE,
            customUrlScheme = BuildConfig.CUSTOM_URL_SCHEME,
            httpProxyPort = BuildConfig.HTTP_PROXY_PORT,
            httpProxyUrl = BuildConfig.HTTP_PROXY_URL,
            supportEmail = BuildConfig.SUPPORT_EMAIL,
            teamsUrl = BuildConfig.TEAMS_URL,
            backendUrl = BuildConfig.BACKEND_URL,
            accountUrl = BuildConfig.ACCOUNT_URL,
            maxAccount = BuildConfig.MAX_ACCOUNTS,
            newPasswordMaximumLength = BuildConfig.NEW_PASSWORD_MAXIMUM_LENGTH,
            newPasswordMinimumLength = BuildConfig.NEW_PASSWORD_MINIMUM_LENGTH,
            passwordMaxAttempts = BuildConfig.PASSWORD_MAX_ATTEMPTS,
            appLockTimeout = BuildConfig.APP_LOCK_TIMEOUT,
            // we use upsert, available from SQL3.24, which is supported from Android API30, so for older APIs we have to use SQLCipher
            shouldEncryptData = !BuildConfig.DEBUG || Build.VERSION.SDK_INT < Build.VERSION_CODES.R,
            lowerKeyPackageLimits = BuildConfig.PRIVATE_BUILD,
            lowerKeyingMaterialsUpdateThreshold = BuildConfig.PRIVATE_BUILD,
            developmentApiEnabled = BuildConfig.DEVELOPMENT_API_ENABLED
        )
    }


}
