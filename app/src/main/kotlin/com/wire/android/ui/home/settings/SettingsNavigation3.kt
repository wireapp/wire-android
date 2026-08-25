/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.home.settings

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultContractId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

/**
 * Navigation 3 contracts for the settings screens.
 *
 * They live next to the feature and carry the full structured session identity so Metro can
 * resolve the correct session-scoped graph without reading route strings or Android Bundles.
 */
@Serializable
data class SettingsRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/settings_screen"
    }
}

@Serializable
data class AppSettingsRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/app_settings_screen"
    }
}

@Serializable
data class NetworkSettingsRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/network_settings_screen"
    }
}

@Serializable
data class PrivacySettingsRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/privacy_settings_config_screen"
    }
}

@Serializable
data class CustomizationSettingsRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/customization_screen"
    }
}

@Serializable
data class LicensesSettingsRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/licenses_screen"
    }
}

@Serializable
data class DependenciesSettingsRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/dependencies_screen"
    }
}

@Serializable
data class BackupAndRestoreSettingsRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/backup_and_restore_screen"
    }
}

@Serializable
data class AboutThisAppRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/about_this_app_screen"
    }
}

@Serializable
data class MyAccountRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/my_account_screen"
    }
}

@Serializable
data class ChangeEmailRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/change_email_screen"
    }
}

@Serializable
data class VerifyEmailRoute(
    override val sessionId: WireSessionId,
    val newEmail: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        require(newEmail.isNotBlank()) { "The email to verify cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/verify_email_screen"
    }
}

@Serializable
data class ChangeUserColorRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/change_user_color_screen"
    }
}

@Serializable
data class ChangeHandleRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/change_handle_screen"
    }
}

@Serializable
data class ChangeDisplayNameRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/change_display_name_screen"
    }
}

@Serializable
data class SettingsAccountUpdateResult(
    val successful: Boolean,
)

internal val SettingsAccountUpdateResultContract =
    WireNavResultContract<SettingsAccountUpdateResult>(
        WireNavResultContractId("settings.account-update")
    )
