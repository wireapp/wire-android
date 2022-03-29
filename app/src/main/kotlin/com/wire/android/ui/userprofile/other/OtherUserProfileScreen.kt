package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.MoreOptionIcon
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
            UserProfileInfo(
                isLoading = state.isAvatarLoading,
                avatarAssetByteArray = state.avatarAssetByteArray,
                fullName = fullName,
                userName = userName,
                teamName = teamName,
                isEditable = false
            )
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
            MoreOptionIcon({})
        }
    )
}
