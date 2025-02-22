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
package com.wire.android.ui.common.dialogs.calling

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.DialogTextSuffixLink
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType

@Composable
fun CallingFeatureActivatedDialog(onDialogDismiss: () -> Unit) {
    WireDialog(
        title = stringResource(id = R.string.calling_feature_enabled_title_alert),
        text = stringResource(id = R.string.calling_feature_enabled_message_alert),
        onDismiss = onDialogDismiss,
        textSuffixLink = DialogTextSuffixLink(
            linkText = stringResource(R.string.calling_feature_enabled_message_link_alert),
            linkUrl = stringResource(R.string.url_wire_enterprise)
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDialogDismiss,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary
        )
    )
}
