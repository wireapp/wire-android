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

import io.kayan.ConfigDefinition
import io.kayan.ConfigValueKind

enum class FeatureConfigs(val jsonKey: String, val kind: ConfigValueKind) {
    /**
     * General APP Coordinates
     */
    APP_NAME("application_name", ConfigValueKind.STRING), // Currently not being used (?)
    APPLICATION_ID("application_id", ConfigValueKind.STRING), // Currently not being used (?)
    USER_ID("application_user_id", ConfigValueKind.STRING),
    PRIVATE_BUILD("application_is_private_build", ConfigValueKind.BOOLEAN),

    /**
     * Feature flags in general
     */
    ALLOW_CHANGE_OF_EMAIL("allow_email_change", ConfigValueKind.BOOLEAN),
    ALLOW_SSO("allow_sso_authentication_option", ConfigValueKind.BOOLEAN),
    ALLOW_ACCOUNT_CREATION("allow_account_creation", ConfigValueKind.BOOLEAN),
    FILE_RESTRICTION_ENABLED("file_restriction_enabled", ConfigValueKind.BOOLEAN),
    FILE_RESTRICTION_LIST("file_restriction_list", ConfigValueKind.STRING),
    FORCE_CONSTANT_BITRATE_CALLS("force_constant_bitrate_calls", ConfigValueKind.BOOLEAN),
    MAX_ACCOUNTS("max_accounts", ConfigValueKind.INT),
    ENABLE_GUEST_ROOM_LINK("enable_guest_room_link", ConfigValueKind.BOOLEAN),
    UPDATE_APP_URL("update_app_url", ConfigValueKind.STRING),
    ENABLE_BLACKLIST("enable_blacklist", ConfigValueKind.BOOLEAN),
    WEBSOCKET_ENABLED_BY_DEFAULT("websocket_enabled_by_default", ConfigValueKind.BOOLEAN),
    TEAM_APP_LOCK("team_app_lock", ConfigValueKind.BOOLEAN),
    TEAM_APP_LOCK_TIMEOUT("team_app_lock_timeout", ConfigValueKind.INT),
    ENABLE_CROSSPLATFORM_BACKUP("enable_crossplatform_backup", ConfigValueKind.BOOLEAN),
    ENABLE_NEW_REGISTRATION("enable_new_registration", ConfigValueKind.BOOLEAN),
    MLS_READ_RECEIPTS_ENABLED("mls_read_receipts_enabled", ConfigValueKind.BOOLEAN),
    EMM_SUPPORT_ENABLED("emm_support_enabled", ConfigValueKind.BOOLEAN),

    /**
     * Security/Cryptography stuff
     */
    LOWER_KEYPACKAGE_LIMIT("lower_keypackage_limit", ConfigValueKind.BOOLEAN),
    ENCRYPT_PROTEUS_STORAGE("encrypt_proteus_storage", ConfigValueKind.BOOLEAN),
    WIPE_ON_COOKIE_INVALID("wipe_on_cookie_invalid", ConfigValueKind.BOOLEAN),
    WIPE_ON_ROOTED_DEVICE("wipe_on_rooted_device", ConfigValueKind.BOOLEAN),
    WIPE_ON_DEVICE_REMOVAL("wipe_on_device_removal", ConfigValueKind.BOOLEAN),
    SELF_DELETING_MESSAGES("self_deleting_messages", ConfigValueKind.BOOLEAN),
    IGNORE_SSL_CERTIFICATES("ignore_ssl_certificates", ConfigValueKind.BOOLEAN),

    /**
     * 3rd party services API Keys and IDs
     */
    FIREBASE_APP_ID("firebase_app_id", ConfigValueKind.STRING),
    FIREBASE_PUSH_SENDER_ID("firebase_push_sender_id", ConfigValueKind.STRING),
    GOOGLE_API_KEY("google_api_key", ConfigValueKind.STRING),
    FCM_PROJECT_ID("fcm_project_id", ConfigValueKind.STRING),

    /**
     * Development/Logging stuff
     */
    LOGGING_ENABLED("logging_enabled", ConfigValueKind.BOOLEAN),
    DEBUG_SCREEN_ENABLED("debug_screen_enabled", ConfigValueKind.BOOLEAN),
    DEVELOPER_FEATURES_ENABLED("developer_features_enabled", ConfigValueKind.BOOLEAN),
    DEVELOPMENT_API_ENABLED("development_api_enabled", ConfigValueKind.BOOLEAN),
    REPORT_BUG_MENU_ITEM_ENABLED("report_bug_menu_item_enabled", ConfigValueKind.BOOLEAN),

    URL_SUPPORT("url_support", ConfigValueKind.STRING),
    URL_RSS_RELEASE_NOTES("url_rss_release_notes", ConfigValueKind.STRING),
    CONVERSATION_FEEDER_ENABLED("conversation_feeder_enabled", ConfigValueKind.BOOLEAN),
    WIRE_COLOR_SCHEME("wire_color_scheme", ConfigValueKind.STRING),

    /**
     * In runtime, will use these values to determine which backend to use.
     * Alternatively, the user can open a deeplink which will allow them to authenticate in a different backend.
     */
    DEFAULT_BACKEND_URL_ACCOUNTS("default_backend_url_accounts", ConfigValueKind.STRING),
    DEFAULT_BACKEND_URL_BASE_API("default_backend_url_base_api", ConfigValueKind.STRING),
    DEFAULT_BACKEND_URL_BASE_WEBSOCKET("default_backend_url_base_websocket", ConfigValueKind.STRING),
    DEFAULT_BACKEND_URL_TEAM_MANAGEMENT("default_backend_url_teams", ConfigValueKind.STRING),
    DEFAULT_BACKEND_URL_BLACKLIST("default_backend_url_blacklist", ConfigValueKind.STRING),
    DEFAULT_BACKEND_URL_WEBSITE("default_backend_url_website", ConfigValueKind.STRING),
    DEFAULT_BACKEND_TITLE("default_backend_title", ConfigValueKind.STRING),

    CERTIFICATE_PINNING_CONFIG("cert_pinning_config", ConfigValueKind.STRING_LIST_MAP),
    // TODO: Add support for default proxy configs

    IS_PASSWORD_PROTECTED_GUEST_LINK_ENABLED("is_password_protected_guest_link_enabled", ConfigValueKind.BOOLEAN),

    SHOULD_DISPLAY_RELEASE_NOTES("should_display_release_notes", ConfigValueKind.BOOLEAN),

    MAX_REMOTE_SEARCH_RESULT_COUNT("max_remote_search_result_count", ConfigValueKind.INT),
    LIMIT_TEAM_MEMBERS_FETCH_DURING_SLOW_SYNC("limit_team_members_fetch_during_slow_sync", ConfigValueKind.INT),

    PICTURE_IN_PICTURE_ENABLED("picture_in_picture_enabled", ConfigValueKind.BOOLEAN),
    PAGINATED_CONVERSATION_LIST_ENABLED("paginated_conversation_list_enabled", ConfigValueKind.BOOLEAN),

    PUBLIC_CHANNELS_ENABLED("public_channels_enabled", ConfigValueKind.BOOLEAN),
    CHANNELS_HISTORY_OPTIONS_ENABLED("channels_history_options_enabled", ConfigValueKind.BOOLEAN),

    USE_NEW_LOGIN_FOR_DEFAULT_BACKEND("use_new_login_for_default_backend", ConfigValueKind.BOOLEAN),
    /**
     * Anonymous Analytics
     */
    ANALYTICS_ENABLED("analytics_enabled", ConfigValueKind.BOOLEAN),
    ANALYTICS_APP_KEY("analytics_app_key", ConfigValueKind.STRING),
    ANALYTICS_SERVER_URL("analytics_server_url", ConfigValueKind.STRING),
    IS_MLS_RESET_ENABLED("is_mls_reset_enabled", ConfigValueKind.BOOLEAN),
    USE_STRICT_MLS_FILTER("use_strict_mls_filter", ConfigValueKind.BOOLEAN),
    MEETINGS_ENABLED("meetings_enabled", ConfigValueKind.BOOLEAN),

    USE_ASYNC_FLUSH_LOGGING("use_async_flush_logging", ConfigValueKind.BOOLEAN),

    /**
     * Background notification retry logic
     * Enables retry with exponential backoff for background notification sync failures
     */
    BACKGROUND_NOTIFICATION_RETRY_ENABLED("background_notification_retry_enabled", ConfigValueKind.BOOLEAN),

    /**
     * Extended stay-alive duration (in seconds) when background notification retry is enabled
     * Controls how long the sync connection stays alive after receiving a push notification
     */
    BACKGROUND_NOTIFICATION_STAY_ALIVE_SECONDS("background_notification_stay_alive_seconds", ConfigValueKind.INT),

    IS_BUBBLE_UI_ENABLED("is_bubble_ui_enabled", ConfigValueKind.BOOLEAN),

    COLLABORA_INTEGRATION_ENABLED("collabora_integration", ConfigValueKind.BOOLEAN),

    DB_INVALIDATION_CONTROL_ENABLED("db_invalidation_control_enabled", ConfigValueKind.BOOLEAN),

    CONFIGURATION_SIGNATURE_KEYS("configuration_signature_keys", ConfigValueKind.STRING_LIST),

    ENFORCE_CONFIGURATION_SIGNATURE("enforce_configuration_signature", ConfigValueKind.BOOLEAN),

    NOMAD_PROFILES_ENABLED("nomad_profiles_enabled", ConfigValueKind.BOOLEAN),

    CALL_QUALITY_MENU_ENABLED("call_quality_menu_enabled", ConfigValueKind.BOOLEAN),

    CALL_REACTIONS_ENABLED("call_reactions_enabled", ConfigValueKind.BOOLEAN);

    val buildConfigType: String
        get() = when (kind) {
            ConfigValueKind.STRING,
            ConfigValueKind.ENUM -> "String"
            ConfigValueKind.BOOLEAN -> "Boolean"
            ConfigValueKind.INT -> "int"
            ConfigValueKind.LONG -> "long"
            ConfigValueKind.DOUBLE -> "double"
            ConfigValueKind.STRING_MAP -> "java.util.HashMap<String, String>"
            ConfigValueKind.STRING_LIST_MAP -> "java.util.HashMap<String, java.util.List<String>>"
            ConfigValueKind.STRING_LIST -> "java.util.List<String>"
        }

    fun toConfigDefinition(): ConfigDefinition = ConfigDefinition(
        jsonKey = jsonKey,
        propertyName = name,
        kind = kind,
        nullable = true
    )
}

fun serializedFeatureConfigsSchemaEntries(requiredConfigs: Set<FeatureConfigs> = emptySet()): List<String> =
    FeatureConfigs.entries.map { config ->
        val isRequired = config in requiredConfigs
        """
            {"jsonKey":"${config.jsonKey.jsonEscape()}","propertyName":"${config.name.jsonEscape()}","kind":"${config.kind.name}","required":$isRequired,"nullable":${!isRequired}}
        """.trimIndent()
    }

private fun String.jsonEscape(): String = buildString {
    this@jsonEscape.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
}
