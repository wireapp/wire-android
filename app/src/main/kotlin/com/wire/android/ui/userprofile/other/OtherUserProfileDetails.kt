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
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.CopyButton
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun OtherUserProfileDetails(
    lazyListState: LazyListState = rememberLazyListState(),
    email: String,
    phoneNumber: String,
    onCopy: (String) -> Unit
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        if (email.isNotEmpty())
            item(key = "user_details_email") {
                UserDetailInformation(
                    title = stringResource(R.string.email_label),
                    value = email,
                    onCopy = onCopy
                )
            }
        if (phoneNumber.isNotEmpty())
            item(key = "user_details_phone") {
                UserDetailInformation(
                    title = stringResource(R.string.phone_label),
                    value = phoneNumber,
                    onCopy = onCopy
                )
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
        actions = { CopyButton(onCopyClicked = { onCopy(value) }) },
        clickable = Clickable(enabled = false, onClick = {})
    )
}

//@Composable
//@Preview
//fun OtherUserProfileDetailsPreview() {
//    OtherUserProfileDetails(OtherUserProfileState.PREVIEW)
//}
