/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package customization

enum class ConfigType(val type: String) {
    STRING("String"),
    BOOLEAN("Boolean"),
    INT("int"),
    MapOfStringToListOfStrings("java.util.HashMap<String, java.util.List<String>>")
}

enum class FeatureConfigs(val value: String, val configType: ConfigType) {
    /**
     * General APP Coordinates
     */
    APP_NAME("application_name", ConfigType.STRING), // Currently not being used (?)
    APPLICATION_ID("application_id", ConfigType.STRING), // Currently not being used (?)
    USER_ID("application_user_id", ConfigType.STRING),
    PRIVATE_BUILD("application_is_private_build", ConfigType.BOOLEAN),

    /**
     * Feature flags in general
     */
    ALLOW_CHANGE_OF_EMAIL("allow_email_change", ConfigType.BOOLEAN),
    ALLOW_SSO("allow_sso_authentication_option", ConfigType.BOOLEAN),
    ALLOW_ACCOUNT_CREATION("allow_account_creation", ConfigType.BOOLEAN),
    FILE_RESTRICTION_ENABLED("file_restriction_enabled", ConfigType.BOOLEAN),
    FILE_RESTRICTION_LIST("file_restriction_list", ConfigType.STRING),
    FORCE_CONSTANT_BITRATE_CALLS("force_constant_bitrate_calls", ConfigType.BOOLEAN),
    MAX_ACCOUNTS("max_accounts", ConfigType.INT),
    ENABLE_GUEST_ROOM_LINK("enable_guest_room_link", ConfigType.BOOLEAN),
    UPDATE_APP_URL("update_app_url", ConfigType.STRING),
    ENABLE_BLACKLIST("enable_blacklist", ConfigType.BOOLEAN),
    WEBSOCKET_ENABLED_BY_DEFAULT("websocket_enabled_by_default", ConfigType.BOOLEAN),
    TEAM_APP_LOCK("team_app_lock", ConfigType.BOOLEAN),
    TEAM_APP_LOCK_TIMEOUT("team_app_lock_timeout", ConfigType.INT),
    ENABLE_CROSSPLATFORM_BACKUP("enable_crossplatform_backup", ConfigType.BOOLEAN),
    ENABLE_NEW_REGISTRATION("enable_new_registration", ConfigType.BOOLEAN),
    MLS_READ_RECEIPTS_ENABLED("mls_read_receipts_enabled", ConfigType.BOOLEAN),
    EMM_SUPPORT_ENABLED("emm_support_enabled", ConfigType.BOOLEAN),

    /**
     * Security/Cryptography stuff
     */
    LOWER_KEYPACKAGE_LIMIT("lower_keypackage_limit", ConfigType.BOOLEAN),
    ENCRYPT_PROTEUS_STORAGE("encrypt_proteus_storage", ConfigType.BOOLEAN),
    WIPE_ON_COOKIE_INVALID("wipe_on_cookie_invalid", ConfigType.BOOLEAN),
    WIPE_ON_ROOTED_DEVICE("wipe_on_rooted_device", ConfigType.BOOLEAN),
    WIPE_ON_DEVICE_REMOVAL("wipe_on_device_removal", ConfigType.BOOLEAN),
    SELF_DELETING_MESSAGES("self_deleting_messages", ConfigType.BOOLEAN),
    IGNORE_SSL_CERTIFICATES("ignore_ssl_certificates", ConfigType.BOOLEAN),

    /**
     * 3rd party services API Keys and IDs
     */
    FIREBASE_APP_ID("firebase_app_id", ConfigType.STRING),
    FIREBASE_PUSH_SENDER_ID("firebase_push_sender_id", ConfigType.STRING),
    GOOGLE_API_KEY("google_api_key", ConfigType.STRING),
    FCM_PROJECT_ID("fcm_project_id", ConfigType.STRING),

    /**
     * Development/Logging stuff
     */
    LOGGING_ENABLED("logging_enabled", ConfigType.BOOLEAN),
    DEBUG_SCREEN_ENABLED("debug_screen_enabled", ConfigType.BOOLEAN),
    DEVELOPER_FEATURES_ENABLED("developer_features_enabled", ConfigType.BOOLEAN),
    DEVELOPMENT_API_ENABLED("development_api_enabled", ConfigType.BOOLEAN),
    REPORT_BUG_MENU_ITEM_ENABLED("report_bug_menu_item_enabled", ConfigType.BOOLEAN),

    URL_SUPPORT("url_support", ConfigType.STRING),
    URL_RSS_RELEASE_NOTES("url_rss_release_notes", ConfigType.STRING),

    /**
     * In runtime, will use these values to determine which backend to use.
     * Alternatively, the user can open a deeplink which will allow them to authenticate in a different backend.
     */
    DEFAULT_BACKEND_URL_ACCOUNTS("default_backend_url_accounts", ConfigType.STRING),
    DEFAULT_BACKEND_URL_BASE_API("default_backend_url_base_api", ConfigType.STRING),
    DEFAULT_BACKEND_URL_BASE_WEBSOCKET("default_backend_url_base_websocket", ConfigType.STRING),
    DEFAULT_BACKEND_URL_TEAM_MANAGEMENT("default_backend_url_teams", ConfigType.STRING),
    DEFAULT_BACKEND_URL_BLACKLIST("default_backend_url_blacklist", ConfigType.STRING),
    DEFAULT_BACKEND_URL_WEBSITE("default_backend_url_website", ConfigType.STRING),
    DEFAULT_BACKEND_TITLE("default_backend_title", ConfigType.STRING),

    CERTIFICATE_PINNING_CONFIG("cert_pinning_config", ConfigType.MapOfStringToListOfStrings),
    // TODO: Add support for default proxy configs

    IS_PASSWORD_PROTECTED_GUEST_LINK_ENABLED("is_password_protected_guest_link_enabled", ConfigType.BOOLEAN),

    SHOULD_DISPLAY_RELEASE_NOTES("should_display_release_notes", ConfigType.BOOLEAN),

    MAX_REMOTE_SEARCH_RESULT_COUNT("max_remote_search_result_count", ConfigType.INT),
    LIMIT_TEAM_MEMBERS_FETCH_DURING_SLOW_SYNC("limit_team_members_fetch_during_slow_sync", ConfigType.INT),

    PICTURE_IN_PICTURE_ENABLED("picture_in_picture_enabled", ConfigType.BOOLEAN),
    PAGINATED_CONVERSATION_LIST_ENABLED("paginated_conversation_list_enabled", ConfigType.BOOLEAN),

    PUBLIC_CHANNELS_ENABLED("public_channels_enabled", ConfigType.BOOLEAN),
    CHANNELS_HISTORY_OPTIONS_ENABLED("channels_history_options_enabled", ConfigType.BOOLEAN),

    USE_NEW_LOGIN_FOR_DEFAULT_BACKEND("use_new_login_for_default_backend", ConfigType.BOOLEAN),
    /**
     * Anonymous Analytics
     */
    ANALYTICS_ENABLED("analytics_enabled", ConfigType.BOOLEAN),
    ANALYTICS_APP_KEY("analytics_app_key", ConfigType.STRING),
    ANALYTICS_SERVER_URL("analytics_server_url", ConfigType.STRING),
    IS_MLS_RESET_ENABLED("is_mls_reset_enabled", ConfigType.BOOLEAN),
    USE_STRICT_MLS_FILTER("use_strict_mls_filter", ConfigType.BOOLEAN),
    MEETINGS_ENABLED("meetings_enabled", ConfigType.BOOLEAN),

    USE_ASYNC_FLUSH_LOGGING("use_async_flush_logging", ConfigType.BOOLEAN),

    /**
     * Background notification retry logic
     * Enables retry with exponential backoff for background notification sync failures
     */
    BACKGROUND_NOTIFICATION_RETRY_ENABLED("background_notification_retry_enabled", ConfigType.BOOLEAN),

    /**
     * Extended stay-alive duration (in seconds) when background notification retry is enabled
     * Controls how long the sync connection stays alive after receiving a push notification
     */
    BACKGROUND_NOTIFICATION_STAY_ALIVE_SECONDS("background_notification_stay_alive_seconds", ConfigType.INT),

    IS_BUBBLE_UI_ENABLED("is_bubble_ui_enabled", ConfigType.BOOLEAN),
}
