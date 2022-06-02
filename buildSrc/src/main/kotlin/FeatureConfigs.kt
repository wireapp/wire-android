enum class ConfigType(val type: String) {
    STRING("String"),
    BOOLEAN("Boolean"),
    INT("int"),
    CERTIFICATE_PIN("certificatePin"),
    FLAVOUR_CONFIG("flavourConfigs")
}

enum class FeatureConfigs(val value: String, val configType: ConfigType) {
    ACCOUNT_URL("accountsUrl", ConfigType.STRING),
    ALLOW_CHANGE_OF_EMAIL("allowChangeOfEmail", ConfigType.BOOLEAN),
    ALLOW_MARKETING_COMMUNICATION("allowMarketingCommunication", ConfigType.BOOLEAN),
    ALLOW_SSO("allowSSO", ConfigType.BOOLEAN),
    ALLOW_ACCOUNT_CREATION("allow_account_creation", ConfigType.BOOLEAN),
    APP_LOCK_TIMEOUT("app_lock_timeout", ConfigType.INT),
    BACKEND_URL("backendUrl", ConfigType.STRING),
    BLACKLIST_HOST("blacklistHost", ConfigType.STRING),
    BLOCK_ON_JAILBREAK_OR_ROOT("block_on_jailbreak_or_root", ConfigType.BOOLEAN),
    BLOCK_ON_PASSWORD_POLICY("block_on_password_policy", ConfigType.BOOLEAN),
    COUNTLY_APP_KEY("countly_app_key", ConfigType.STRING),
    COUNTLY_SERVER_URL("countly_server_url", ConfigType.STRING),
    CUSTOM_URL_SCHEME("custom_url_scheme", ConfigType.STRING),
    ENABLE_BLACK_LIST("enableBlacklist", ConfigType.BOOLEAN),
    FILE_RESTRICTION_ENABLED("file_restriction_enabled", ConfigType.BOOLEAN),
    FILE_RESTRICTION_LIST("file_restriction_list", ConfigType.STRING),
    FIREBASE_APP_ID("firebaseAppId", ConfigType.STRING),
    FIREBASE_PUSH_SENDER_ID("firebasePushSenderId", ConfigType.STRING),
    FORCE_APP_LOCK("force_app_lock", ConfigType.BOOLEAN),
    FORCE_CONSTANT_BITRATE_CALLS("force_constant_bitrate_calls", ConfigType.BOOLEAN),
    FORCE_HIDE_SCREEN_CONTENT("force_hide_screen_content", ConfigType.BOOLEAN),
    FORCE_PRIVATE_KEYBOARD("force_private_keyboard", ConfigType.BOOLEAN),
    HTTP_PROXY_PORT("http_proxy_port", ConfigType.STRING),
    HTTP_PROXY_URL("http_proxy_url", ConfigType.STRING),
    KEEP_WEB_SOCKET_ON("keep_websocket_on", ConfigType.BOOLEAN),
    MAX_ACCOUNTS("maxAccounts", ConfigType.INT),
    NEW_PASSWORD_MAXIMUM_LENGTH("new_password_maximum_length", ConfigType.INT),
    NEW_PASSWORD_MINIMUM_LENGTH("new_password_minimum_length", ConfigType.INT),
    PASSWORD_MAX_ATTEMPTS("password_max_attempts", ConfigType.INT),
    WIPE_ON_COOKIE_INVALID("wipe_on_cookie_invalid", ConfigType.BOOLEAN),
    SUBMIT_CRASH_REPORTS("submitCrashReports", ConfigType.BOOLEAN),
    SUPPORT_EMAIL("supportEmail", ConfigType.STRING),
    TEAMS_URL("teamsUrl", ConfigType.STRING),
    WEB_LINK_PREVIEW("web_link_preview", ConfigType.BOOLEAN),
    WEBSITE_URL("websiteUrl", ConfigType.STRING),
    WEB_SOCKET_URL("websocketUrl", ConfigType.STRING),

    CERTIFICATE_PIN("certificatePin", ConfigType.CERTIFICATE_PIN),


    CANDIDATE("candidate", ConfigType.FLAVOUR_CONFIG),
    EXPERIMENTAL("experimental", ConfigType.FLAVOUR_CONFIG),
    FDROID("fdroid", ConfigType.FLAVOUR_CONFIG),
    DEV("dev", ConfigType.FLAVOUR_CONFIG),
    INTERNAL("internal", ConfigType.FLAVOUR_CONFIG),
    PROD("prod", ConfigType.FLAVOUR_CONFIG);

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
    USER_ID("userId", ConfigType.STRING);

}
