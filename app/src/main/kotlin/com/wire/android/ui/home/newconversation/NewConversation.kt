package com.wire.android.ui.home.newconversation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension.Companion.fillToConstraints
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.NavigableSearchBar
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.home.conversationslist.common.RowItem
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan


@Composable
fun NewConversationScreen(newConversationViewModel: NewConversationViewModel = hiltViewModel()) {
    val state by newConversationViewModel.newConversationState

    NewConversationContent(
        state = state,
        onCloseClick = { newConversationViewModel.close() }
    )
}

@Composable
fun NewConversationContent(
    state: NewConversationState,
    onCloseClick: () -> Unit
) {
    val lazyListState = rememberLazyListState()

    ConstraintLayout(Modifier.fillMaxSize()) {

        val (topBar, content) = createRefs()

        var isCollapsed by remember {
            mutableStateOf(false)
        }

        var isTopBarVisible by remember {
            mutableStateOf(true)
        }

        LaunchedEffect(lazyListState.firstVisibleItemIndex) {
            snapshotFlow { lazyListState.firstVisibleItemIndex }
                .scan(0 to 0) { prevPair, newScrollIndex ->
                    if (prevPair.second == newScrollIndex || newScrollIndex == prevPair.second + 1) prevPair
                    else prevPair.second to newScrollIndex
                }
                .map { (prevScrollIndex, newScrollIndex) ->
                    newScrollIndex > prevScrollIndex + 1
                }
                .distinctUntilChanged().collect {
                    isCollapsed = it
                }
        }

        val searchFieldFullHeightPx = LocalDensity.current.run {
            (dimensions().topBarSearchFieldHeight).toPx()
        }

        val searchFieldPosition by animateFloatAsState(if (isCollapsed) 0f else searchFieldFullHeightPx)

        Box(modifier = Modifier
            .constrainAs(topBar) {
                top.linkTo(parent.top)
                bottom.linkTo(content.top)
            }
            .wrapContentSize()) {
            Test(searchFieldPosition)
        }

//        Box(
//            Modifier
//                .background(Color.Red)
//                .wrapContentSize()
//        ) {
//            Surface(
//                modifier = Modifier
//                    .height(dimensions().topBarSearchFieldHeight + 32.dp)
//                    .graphicsLayer { translationY = searchFieldPosition },
//                shadowElevation = dimensions().topBarElevationHeight
//            ) {
//                val interactionSource = remember {
//                    MutableInteractionSource()
//                }
//
//                if (interactionSource.collectIsPressedAsState().value) {
//                    isTopBarVisible = !isTopBarVisible
//                }
//
//                NavigableSearchBar(
//                    placeholderText = "Search people",
//                    onNavigateBack = { },
//                    interactionSource = interactionSource
//                )
//            }
//
//            AnimatedVisibility(visible = isTopBarVisible) {
//                WireCenterAlignedTopAppBar(
//                    elevation = if (isCollapsed) dimensions().topBarElevationHeight else 0.dp,
//                    title = stringResource(R.string.label_new_conversation),
//                    navigationIconType = NavigationIconType.Close,
//                    onNavigationPressed = onCloseClick
//                )
//            }
//        }

        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentSize()
                .constrainAs(content) {
                    top.linkTo(topBar.bottom)
                    bottom.linkTo(parent.bottom)

                    height = fillToConstraints
                }
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
            ) {
                folderWithElements(
                    header = { stringResource(R.string.label_contacts) },
                    items = state.contacts
                ) { contact ->
                    ContactItem(
                        contact.name,
                        contact.userStatus,
                        contact.avatarUrl
                    )
                }
            }
            Divider()
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(all = 16.dp)
            ) {
                WirePrimaryButton(
                    text = stringResource(R.string.label_new_group),
                    onClick = {
                        //TODO:open new group screen
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                WirePrimaryButton(
                    text = stringResource(R.string.label_new_guestroom),
                    onClick = {
                        //TODO:open new guestroom
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun Test(searchFieldPosition: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Surface(
            modifier = Modifier
                .height(searchFieldPosition.dp)
                .background(Color.Yellow)
                .wrapContentWidth(),
            shadowElevation = dimensions().topBarElevationHeight
        ) {
            val interactionSource = remember {
                MutableInteractionSource()
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Yellow)
            ) {
                NavigableSearchBar(
                    placeholderText = "Search people",
                    onNavigateBack = { },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                )
            }

//            if (interactionSource.collectIsPressedAsState().value) {
//                isTopBarVisible = !isTopBarVisible
//            }

        }
//        AnimatedVisibility(visible = isTopBarVisible) {
//        WireCenterAlignedTopAppBar(
//            elevation = 0.dp,
//            title = stringResource(R.string.label_new_conversation),
//            navigationIconType = NavigationIconType.Close,
//            onNavigationPressed = { }
//        )
//        }
    }
}

@Composable
private fun ContactItem(
    name: String,
    status: UserStatus,
    avatarUrl: String
) {
    RowItem({
        //TODO: Open Contact Screen
    }, {
        //TODO: Show Context Menu ?
    }, {
        UserProfileAvatar(
            avatarUrl = avatarUrl,
            status = status
        )
        Text(
            text = name,
            style = MaterialTheme.wireTypography.title02,
        )
    })
}
