package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.userprofile.common.UserProfileInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(viewModel: OtherUserProfileScreenViewModel = hiltViewModel()) {
    val state = viewModel.state

    Scaffold(
        topBar = {
            OtherUserProfileTopBar(onNavigateBack = { viewModel.navigateBack() })
        },
    ) {
        OtherProfileScreenContent(state)
    }
}

@Composable
fun OtherProfileScreenContent(state: OtherUserProfileScreenState) {
    with(state) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(Modifier.weight(1f)) {
                item {
                    UserProfileInfo(
                        isLoading = state.isAvatarLoading,
                        avatarAssetByteArray = state.avatarAssetByteArray,
                        fullName = fullName,
                        userName = userName,
                        teamName = teamName,
                        isEditable = false
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        WireSecondaryButton(
                            onClick = { /*TODO*/ },
                            text = stringResource(R.string.welcome_button_create_team),
                            fillMaxWidth = false
                        )
                        WireSecondaryButton(
                            onClick = { /*TODO*/ },
                            text = stringResource(R.string.welcome_button_create_team),
                            fillMaxWidth = false
                        )
                    }
                }
            }
            Divider()
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(dimensions().groupButtonHeight)
                    .fillMaxWidth()
                    .padding(all = dimensions().spacing16x)
            ) {
                WirePrimaryButton(
                    text = "Open Conversation",
                    onClick = {
                        //TODO:redirect to conversation
                    },
                )
            }
        }
    }
}

@Composable
fun OtherUserProfileTopBar(onNavigateBack: () -> Unit) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        title = stringResource(id = R.string.user_profile_title),
        elevation = 0.dp,
        actions = {
            MoreOptionIcon({ })
        }
    )
}
