package com.wire.android.ui.userprofile.other

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfile() {
    Scaffold(
        topBar = {
            OtherUserProfileTopBar(

            )
        },
    ) {

    }
}

@Composable
fun OtherUserProfileTopBar() {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = { },
        title = stringResource(id = R.string.user_profile_title),
        actions = {
            WireSecondaryButton(
                onClick = { },
                text = stringResource(R.string.user_profile_logout),
                fillMaxWidth = false,
                minHeight = dimensions().userProfileLogoutBtnHeight,
                state = WireButtonState.Error
            )
        }
    )
}
