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

package com.wire.android.util

import android.content.res.Resources
import androidx.compose.ui.text.AnnotatedString
import com.wire.android.R
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure

fun CoreFailure.dialogErrorStrings(resources: Resources): DialogErrorStrings = when (this) {
    is NetworkFailure.NoNetworkConnection -> DialogErrorStrings(
        resources.getString(R.string.error_no_network_title),
        resources.getString(R.string.error_no_network_message)
    )

    is NetworkFailure.ServerMiscommunication -> DialogErrorStrings(
        resources.getString(R.string.error_server_miscommunication_title),
        resources.getString(R.string.error_server_miscommunication_message)
    )

    else -> DialogErrorStrings(
        resources.getString(R.string.error_unknown_title),
        resources.getString(R.string.error_unknown_message)
    )
}

fun CoreFailure.uiText(): UIText = when (this) {
    is NetworkFailure.NoNetworkConnection -> UIText.StringResource(R.string.error_no_network_message)
    is NetworkFailure.ServerMiscommunication -> UIText.StringResource(R.string.error_server_miscommunication_message)
    else -> UIText.StringResource(R.string.error_unknown_message)
}

data class DialogErrorStrings(val title: String, val message: String)
data class DialogAnnotatedErrorStrings(val title: String, val annotatedMessage: AnnotatedString)
