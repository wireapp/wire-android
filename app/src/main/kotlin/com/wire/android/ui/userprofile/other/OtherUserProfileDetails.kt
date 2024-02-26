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

package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.CopyButton
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun OtherUserProfileDetails(
    state: OtherUserProfileState,
    otherUserProfileScreenState: OtherUserProfileScreenState = rememberOtherUserProfileScreenState(),
    lazyListState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "user_details_domain") {
            UserDetailInformation(
                title = stringResource(R.string.settings_myaccount_domain),
                value = state.userId.domain,
                onCopy = null
            )
        }
        if (state.email.isNotEmpty()) {
            item(key = "user_details_email") {
                UserDetailInformation(
                    title = stringResource(R.string.email_label),
                    value = state.email,
                    onCopy = { otherUserProfileScreenState.copy(it, context) }
                )
            }
        }
        if (state.phone.isNotEmpty()) {
            item(key = "user_details_phone") {
                UserDetailInformation(
                    title = stringResource(R.string.phone_label),
                    value = state.phone,
                    onCopy = { otherUserProfileScreenState.copy(it, context) }
                )
            }
        }
    }
}

@Composable
private fun UserDetailInformation(
    title: String,
    value: String,
    onCopy: ((String) -> Unit)?
) {
    RowItemTemplate(
        modifier = Modifier.padding(horizontal = dimensions().spacing8x),
        title = {
            Text(
                style = MaterialTheme.wireTypography.subline01,
                color = MaterialTheme.wireColorScheme.labelText,
                text = title.uppercase()
            )
        },
        subtitle = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = value
            )
        },
        actions = { onCopy?.let { CopyButton(onCopyClicked = { onCopy(value) }) } },
        clickable = Clickable(enabled = false) {}
    )
}

@Composable
@Preview
fun PreviewOtherUserProfileDetails() {
    OtherUserProfileDetails(OtherUserProfileState.PREVIEW)
}
