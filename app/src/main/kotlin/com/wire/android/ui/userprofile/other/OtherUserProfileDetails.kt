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

import androidx.annotation.StringRes
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
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.CopyButton
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun OtherUserProfileDetails(
    state: OtherUserProfileState,
    modifier: Modifier = Modifier,
    otherUserProfileScreenState: OtherUserProfileScreenState = rememberOtherUserProfileScreenState(),
    lazyListState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "user_details_domain") {
            UserDetailInformation(
                title = stringResource(R.string.settings_myaccount_domain),
                value = state.userId.domain,
                onCopy = null,
                copyBtnContentDescription = null
            )
        }
        if (state.email.isNotEmpty()) {
            item(key = "user_details_email") {
                UserDetailInformation(
                    title = stringResource(R.string.email_label),
                    value = state.email,
                    onCopy = { otherUserProfileScreenState.copy(it, context) },
                    copyBtnContentDescription = R.string.content_description_user_profile_copy_email_btn
                )
            }
        }
        if (state.phone.isNotEmpty()) {
            item(key = "user_details_phone") {
                UserDetailInformation(
                    title = stringResource(R.string.phone_label),
                    value = state.phone,
                    onCopy = { otherUserProfileScreenState.copy(it, context) },
                    copyBtnContentDescription = R.string.content_description_user_profile_copy_phone_btn
                )
            }
        }
    }
}

@Composable
private fun UserDetailInformation(
    title: String,
    value: String,
    onCopy: ((String) -> Unit)?,
    @StringRes copyBtnContentDescription: Int?
) {
    RowItemTemplate(
        modifier = Modifier.padding(horizontal = dimensions().spacing8x),
        title = {
            Text(
                style = MaterialTheme.wireTypography.label01,
                color = MaterialTheme.wireColorScheme.secondaryText,
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
        actions = {
            onCopy?.let {
                CopyButton(
                    onCopyClicked = { onCopy(value) },
                    contentDescription = copyBtnContentDescription ?: R.string.content_description_copy
                )
            }
        },
        clickable = Clickable(enabled = false) {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewOtherUserProfileDetails() = WireTheme {
    OtherUserProfileDetails(OtherUserProfileState.PREVIEW)
}
