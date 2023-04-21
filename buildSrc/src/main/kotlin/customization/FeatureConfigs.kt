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
 */
package customization

enum class ConfigType(val type: String) {
    STRING("String"),
    BOOLEAN("Boolean"),
    INT("int"),
}

enum class FeatureConfigs(val value: String, val configType: ConfigType) {
    ALLOW_CHANGE_OF_EMAIL("allowChangeOfEmail", ConfigType.BOOLEAN),
    ALLOW_MARKETING_COMMUNICATION("allowMarketingCommunication", ConfigType.BOOLEAN),
    ALLOW_SSO("allowSSO", ConfigType.BOOLEAN),
    ALLOW_ACCOUNT_CREATION("allow_account_creation", ConfigType.BOOLEAN),
    SUPPORT_URL("webSiteUrl", ConfigType.STRING),
    ACCOUNTS_URL("accountsUrl", ConfigType.STRING),
    BACKEND_URL("backendUrl", ConfigType.STRING),
    BLACKLIST_HOST("blacklistHost", ConfigType.STRING),
    BLOCK_ON_JAILBREAK_OR_ROOT("block_on_jailbreak_or_root", ConfigType.BOOLEAN),
    BLOCK_ON_PASSWORD_POLICY("block_on_password_policy", ConfigType.BOOLEAN),
    CUSTOM_URL_SCHEME("custom_url_scheme", ConfigType.STRING),
    FILE_RESTRICTION_ENABLED("file_restriction_enabled", ConfigType.BOOLEAN),
    MLS_SUPPORT_ENABLED("mls_support_enabled", ConfigType.BOOLEAN),
    FORCE_APP_LOCK("force_app_lock", ConfigType.BOOLEAN),
    FORCE_CONSTANT_BITRATE_CALLS("force_constant_bitrate_calls", ConfigType.BOOLEAN),
    FORCE_HIDE_SCREEN_CONTENT("force_hide_screen_content", ConfigType.BOOLEAN),
    KEEP_WEB_SOCKET_ON("keep_websocket_on", ConfigType.BOOLEAN),
    MAX_ACCOUNTS("maxAccounts", ConfigType.INT),
    WIPE_ON_COOKIE_INVALID("wipe_on_cookie_invalid", ConfigType.BOOLEAN),
    WIPE_ON_DEVICE_REMOVAL("wipe_on_device_removal", ConfigType.BOOLEAN),
    SUPPORT_EMAIL("supportEmail", ConfigType.STRING),
    TEAMS_URL("teamsUrl", ConfigType.STRING),
    WEB_LINK_PREVIEW("web_link_preview", ConfigType.BOOLEAN),
    ENCRYPT_PROTEUS_STORAGE("encrypt_proteus_storage", ConfigType.BOOLEAN),
    GUEST_ROOM_LINK("guest_room_link", ConfigType.BOOLEAN),
    UPDATE_APP_URL("update_app_url", ConfigType.STRING),
    APP_NAME("appName", ConfigType.STRING),
    APPLICATION_ID("applicationId", ConfigType.STRING),
    LAUNCHER_ICON("launcherIcon", ConfigType.STRING),
    LOGGING_ENABLED("logging_enabled", ConfigType.BOOLEAN),
    SAFE_LOGGING("safe_logging", ConfigType.BOOLEAN),
    PRIVATE_BUILD("private_build", ConfigType.BOOLEAN),
    FIREBASE_APP_ID("firebaseAppId", ConfigType.STRING),
    FIREBASE_PUSH_SENDER_ID("firebasePushSenderId", ConfigType.STRING),
    GOOGLE_API_KEY("googleApiKey", ConfigType.STRING),
    FCM_PROJECT_ID("fcmProjectID", ConfigType.STRING),

    DEVELOPER_FEATURES_ENABLED("developer_features_enabled", ConfigType.BOOLEAN),
    DEVELOPMENT_API_ENABLED("development_api_enabled", ConfigType.BOOLEAN),

    URL_SUPPORT("url_support", ConfigType.STRING),

    DEFAULT_BACKEND_URL_ACCOUNTS("default_backend_url_accounts", ConfigType.STRING),
    DEFAULT_BACKEND_URL_BASE_API("default_backend_url_base_api", ConfigType.STRING),
    DEFAULT_BACKEND_URL_BASE_WEBSOCKET("default_backend_url_base_websocket", ConfigType.STRING),
    DEFAULT_BACKEND_URL_TEAM_MANAGEMENT("default_backend_url_teams", ConfigType.STRING),
    DEFAULT_BACKEND_URL_BLACKLIST("default_backend_url_blacklist", ConfigType.STRING),
    DEFAULT_BACKEND_URL_WEBSITE("default_backend_url_website", ConfigType.STRING),
    DEFAULT_BACKEND_TITLE("default_backend_title", ConfigType.STRING),
    BLACKLIST_ENABLE("enableBlacklist", ConfigType.BOOLEAN),
    // TODO: Add support for default proxy configs

    USER_ID("userId", ConfigType.STRING),
    DEBUG_SCREEN_ENABLED("debug_screen_enabled", ConfigType.BOOLEAN),
    REPORT_BUG_MENU_ITEM_ENABLED("report_bug_menu_item_enabled", ConfigType.BOOLEAN);
}
