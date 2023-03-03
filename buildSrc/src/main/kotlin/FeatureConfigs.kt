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

enum class ConfigType(val type: String) {
    STRING("String"),
    BOOLEAN("Boolean"),
    INT("int"),
    CERTIFICATE_PIN("certificatePin"),
    FLAVOUR_CONFIG("flavourConfigs")
}

enum class FeatureConfigs(val value: String, val configType: ConfigType) {
    ALLOW_CHANGE_OF_EMAIL("allowChangeOfEmail", ConfigType.BOOLEAN),
    ALLOW_MARKETING_COMMUNICATION("allowMarketingCommunication", ConfigType.BOOLEAN),
    ALLOW_SSO("allowSSO", ConfigType.BOOLEAN),
    ALLOW_ACCOUNT_CREATION("allow_account_creation", ConfigType.BOOLEAN),
    BACKEND_URL("backendUrl", ConfigType.STRING),
    BLACKLIST_HOST("blacklistHost", ConfigType.STRING),
    BLOCK_ON_JAILBREAK_OR_ROOT("block_on_jailbreak_or_root", ConfigType.BOOLEAN),
    BLOCK_ON_PASSWORD_POLICY("block_on_password_policy", ConfigType.BOOLEAN),
    COUNTLY_APP_KEY("countly_app_key", ConfigType.STRING),
    COUNTLY_SERVER_URL("countly_server_url", ConfigType.STRING),
    CUSTOM_URL_SCHEME("custom_url_scheme", ConfigType.STRING),
    ENABLE_BLACK_LIST("enableBlacklist", ConfigType.BOOLEAN),
    FILE_RESTRICTION_ENABLED("file_restriction_enabled", ConfigType.BOOLEAN),
    MLS_SUPPORT_ENABLED("mls_support_enabled", ConfigType.BOOLEAN),
    FORCE_APP_LOCK("force_app_lock", ConfigType.BOOLEAN),
    FORCE_CONSTANT_BITRATE_CALLS("force_constant_bitrate_calls", ConfigType.BOOLEAN),
    FORCE_HIDE_SCREEN_CONTENT("force_hide_screen_content", ConfigType.BOOLEAN),
    KEEP_WEB_SOCKET_ON("keep_websocket_on", ConfigType.BOOLEAN),
    MAX_ACCOUNTS("maxAccounts", ConfigType.INT),
    WIPE_ON_COOKIE_INVALID("wipe_on_cookie_invalid", ConfigType.BOOLEAN),
    SUPPORT_EMAIL("supportEmail", ConfigType.STRING),
    TEAMS_URL("teamsUrl", ConfigType.STRING),
    WEB_LINK_PREVIEW("web_link_preview", ConfigType.BOOLEAN),
    WEB_SOCKET_URL("websocketUrl", ConfigType.STRING),
    ENCRYPT_PROTEUS_STORAGE("encrypt_proteus_storage", ConfigType.BOOLEAN),
    GUEST_ROOM_LINK("guest_room_link", ConfigType.BOOLEAN),
    UPDATE_APP_URL("update_app_url", ConfigType.STRING),

    CERTIFICATE_PIN("certificatePin", ConfigType.CERTIFICATE_PIN),


    CANDIDATE("candidate", ConfigType.FLAVOUR_CONFIG),
    EXPERIMENTAL("experimental", ConfigType.FLAVOUR_CONFIG),
    FDROID("fdroid", ConfigType.FLAVOUR_CONFIG),
    DEV("dev", ConfigType.FLAVOUR_CONFIG),
    INTERNAL("internal", ConfigType.FLAVOUR_CONFIG),
    STAGING("staging", ConfigType.FLAVOUR_CONFIG),
    PROD("prod", ConfigType.FLAVOUR_CONFIG),
    BETA("beta", ConfigType.FLAVOUR_CONFIG);
}

enum class CertificatePin(val value: String, val configType: ConfigType) {
    CERTIFICATE("certificate", ConfigType.STRING),
    DOMAIN("domain", ConfigType.STRING);
}

enum class FlavourConfigs(val value: String, val configType: ConfigType) {
    COMMENT("_comment", ConfigType.STRING),
    APP_NAME("appName", ConfigType.STRING),
    APPLICATION_ID("applicationId", ConfigType.STRING),
    DEVELOPER_FEATURES_ENABLED("developer_features_enabled", ConfigType.BOOLEAN),
    LAUNCHER_ICON("launcherIcon", ConfigType.STRING),
    LOGGING_ENABLED("logging_enabled", ConfigType.BOOLEAN),
    SAFE_LOGGING("safe_logging", ConfigType.BOOLEAN),
    PRIVATE_BUILD("private_build", ConfigType.BOOLEAN),
    DEVELOPMENT_API_ENABLED("development_api_enabled", ConfigType.BOOLEAN),
    FIREBASE_APP_ID("firebaseAppId", ConfigType.STRING),
    FIREBASE_PUSH_SENDER_ID("firebasePushSenderId", ConfigType.STRING),
    GOOGLE_API_KEY("googleApiKey", ConfigType.STRING),
    FCM_PROJECT_ID("fcmProjectID", ConfigType.STRING),
    IS_STAGING("isStaging", ConfigType.BOOLEAN),
    USER_ID("userId", ConfigType.STRING);
}
