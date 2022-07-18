package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    otherUserProfileScreenState: OtherUserProfileScreenState = rememberOtherUserProfileScreenState(remember { SnackbarHostState() }),
    lazyListState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.email.isNotEmpty())
            item(key = "user_details_email") {
                UserDetailInformation(
                    title = stringResource(R.string.email_label),
                    value = state.email,
                    onCopy = { otherUserProfileScreenState.copy(it) }
                )
            }
        if (state.phone.isNotEmpty())
            item(key = "user_details_phone") {
                UserDetailInformation(
                    title = stringResource(R.string.phone_label),
                    value = state.phone,
                    onCopy = { otherUserProfileScreenState.copy(it) }
                )
            }
        repeat(20) {
            item(key = "user_details_email$it") {
                UserDetailInformation(
                    title = stringResource(R.string.email_label),
                    value = state.email + it,
                    onCopy = { otherUserProfileScreenState.copy(it) }
                )
            }
        }
    }
}


@Composable
private fun UserDetailInformation(
    title: String,
    value: String,
    onCopy: (String) -> Unit
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
        actions = { CopyButton(onCopyClicked = { onCopy("$value copied") }) },
        clickable = Clickable(enabled = false) {}
    )
}

@Composable
@Preview
fun OtherUserProfileDetailsPreview() {
    OtherUserProfileDetails(OtherUserProfileState.PREVIEW)
}
