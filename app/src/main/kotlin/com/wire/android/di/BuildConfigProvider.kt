package com.wire.android.di

import com.wire.android.BuildConfig
import com.wire.kalium.logic.featureFlags.BuildTimeConfigs
import dagger.Provides
import javax.inject.Singleton

@Singleton
@Provides
fun buildConfigProvider(): BuildTimeConfigs {
    return BuildTimeConfigs(
        isAccountCreationEnabled = BuildConfig.ALLOW_ACCOUNT_CREATION,
        isChangeEmailEnabled = BuildConfig.ALLOW_CHANGE_OF_EMAIL,
        isLoggingEnabled = BuildConfig.LOGGING_ENABLED,
        isMarketingCommunicationEnabled = BuildConfig.ALLOW_MARKETING_COMMUNICATION,
        isSSoEnabled = BuildConfig.ALLOW_SSO,
        blockOnJailbreakOrRoot = BuildConfig.BLOCK_ON_JAILBREAK_OR_ROOT,
        blacklistHost = BuildConfig.BLACKLIST_HOST,
        blockOnPasswordPolicy = BuildConfig.BLOCK_ON_PASSWORD_POLICY,
        fileRestrictionEnabled = BuildConfig.FILE_RESTRICTION_ENABLED,
        fileRestrictionList = BuildConfig.FILE_RESTRICTION_LIST,
        forceAppLock = BuildConfig.FORCE_APP_LOCK,
        forceConstantBitrateCalls = BuildConfig.FORCE_CONSTANT_BITRATE_CALLS,
        forceHideScreenContent = BuildConfig.FORCE_HIDE_SCREEN_CONTENT,
        forcePrivateKeyboard = BuildConfig.FORCE_PRIVATE_KEYBOARD,
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
        countlyAppKey = BuildConfig.COUNTLY_APP_KEY,
        countlyServerUrl = BuildConfig.COUNTLY_SERVER_URL,
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
        appLockTimeout = BuildConfig.APP_LOCK_TIMEOUT
    )
}
