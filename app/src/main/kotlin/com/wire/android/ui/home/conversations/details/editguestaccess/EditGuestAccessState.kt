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

package com.wire.android.ui.home.conversations.details.editguestaccess

data class EditGuestAccessState(
    val isGuestAccessAllowed: Boolean = false,
    val isServicesAccessAllowed: Boolean = false,
    val isGuestRoomLinkFeatureEnabled: Boolean = true,
    val isUpdatingGuestAccessAllowed: Boolean = false,
    val shouldShowGuestAccessChangeConfirmationDialog: Boolean = false,
    val isUpdatingGuestAccess: Boolean = false,
    val isGeneratingGuestRoomLink: Boolean = false,
    val isFailedToGenerateGuestRoomLink: Boolean = false,
    val shouldShowRevokeLinkConfirmationDialog: Boolean = false,
    val isRevokingLink: Boolean = false,
    val isLinkCopied: Boolean = false,
    val isFailedToRevokeGuestRoomLink: Boolean = false,
    val link: String? = null,
    val isLinkPasswordProtected: Boolean = false,
    val shouldShowPasswordDialog: Boolean = false,
    val isPasswordProtectedLinksAllowed: Boolean = false
)
