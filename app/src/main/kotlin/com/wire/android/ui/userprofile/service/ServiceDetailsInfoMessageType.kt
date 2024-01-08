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
package com.wire.android.ui.userprofile.service

import com.wire.android.R
import com.wire.android.model.SnackBarMessage
import com.wire.android.util.ui.UIText

sealed class ServiceDetailsInfoMessageType(override val uiText: UIText) : SnackBarMessage {

    // Remove Service
    object SuccessRemoveService : ServiceDetailsInfoMessageType(UIText.StringResource(R.string.service_remove_success))
    object ErrorRemoveService : ServiceDetailsInfoMessageType(UIText.StringResource(R.string.service_remove_error))

    // Add Service
    object SuccessAddService : ServiceDetailsInfoMessageType(UIText.StringResource(R.string.service_add_success))
    object ErrorAddService : ServiceDetailsInfoMessageType(UIText.StringResource(R.string.service_add_error))
}
