package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.theme.WireTheme

@Composable
fun GroupConversationAllParticipantsScreen(viewModel: GroupConversationParticipantsViewModel) {
    GroupConversationAllParticipantsContent(
        onBackPressed = viewModel::navigateBack,
        groupParticipantsState = viewModel.groupParticipantsState,
        onProfilePressed = viewModel::openProfile
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun GroupConversationAllParticipantsContent(
    onBackPressed: () -> Unit,
    onProfilePressed: (UIParticipant) -> Unit,
    groupParticipantsState: GroupConversationParticipantsState
) {
    val lazyListState: LazyListState = rememberLazyListState()
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = lazyListState.rememberTopBarElevationState().value,
                title = stringResource(R.string.conversation_details_group_participants_title),
                navigationIconType = NavigationIconType.Back,
                onNavigationPressed = onBackPressed
            ) {
                // TODO add search bar
            }
        },
        modifier = Modifier.fillMaxHeight(),
    ) { internalPadding ->
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            val context = LocalContext.current
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth().padding(internalPadding)
            ) {
                participantsFoldersWithElements(context, groupParticipantsState, onProfilePressed)
            }
        }
    }
}

@Preview
@Composable
private fun GroupConversationAllParticipantsPreview() {
    WireTheme(isPreview = true) {
        GroupConversationAllParticipantsContent({}, {}, GroupConversationParticipantsState.PREVIEW)
    }
}
