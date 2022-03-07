package com.wire.android.ui.home.newconversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.NavigationIconType
import com.wire.android.ui.common.SearchBarInput
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

@Composable
fun DeprecatedSearchTopBar(
    topBarTitle: String,
    scrollPosition: Int,
    navigationIconType: NavigationIconType = NavigationIconType.Back,
    onNavigationPressed: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    searchBar: @Composable () -> Unit,
) {
    var isCollapsed: Boolean by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(scrollPosition) {
        snapshotFlow { scrollPosition }
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
        (dimensions().topBarSearchFieldHeight + dimensions().topBarElevationHeight).toPx()
    }

    val searchFieldPosition by animateFloatAsState(if (isCollapsed) -searchFieldFullHeightPx else 0f)

    Box(
        Modifier.background(Color.Transparent)
    ) {
        Surface(
            modifier = Modifier
                .padding(top = dimensions().smallTopBarHeight)
                .height(dimensions().topBarSearchFieldHeight)
                .graphicsLayer { translationY = searchFieldPosition },
            shadowElevation = dimensions().topBarElevationHeight
        ) {
            searchBar()
        }

        WireCenterAlignedTopAppBar(
            elevation = if (isCollapsed) dimensions().topBarElevationHeight else 0.dp,
            title = topBarTitle,
            navigationIconType = navigationIconType,
            onNavigationPressed = onNavigationPressed,
            actions = actions
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppTopBarWithSearchBar(
    scrollPosition: Int,
    navigationIconType: NavigationIconType,
    searchBarHint: String,
    topBarTitle: String,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onSearchClicked: () -> Unit = {},
    onCloseSearchClicked: () -> Unit = {},
    onNavigateBackClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val searchBarState = rememberSearchbarState(scrollPosition)

    AppTopBarWithSearchBarContent(
        searchBarState,
        navigationIconType = navigationIconType,
        searchBarHint = searchBarHint,
        topBarTitle = topBarTitle,
        searchQuery = searchQuery,
        onSearchQueryChanged = {
            onSearchQueryChanged(it)
        },
        onInputClicked = onSearchClicked,
        onCloseSearchClicked = onCloseSearchClicked,
        onNavigateBackClicked = onNavigateBackClicked,
        modifier = modifier
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AppTopBarWithSearchBarContent(
    searchBarState: SearchBarState,
    navigationIconType: NavigationIconType,
    searchBarHint: String,
    topBarTitle: String,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onInputClicked: () -> Unit,
    onCloseSearchClicked: () -> Unit,
    onNavigateBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    with(searchBarState) {
        val animatedTopBarTotalHeight by animateFloatAsState(size)

        Box(
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Surface(
                modifier = Modifier
                    .height(animatedTopBarTotalHeight.dp)
                    .wrapContentWidth(),
                shadowElevation = if (isCollapsed) dimensions().topBarElevationHeight else 0.dp
            ) {
                val interactionSource = remember {
                    MutableInteractionSource()
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.wireColorScheme.background)
                ) {
                    val focusManager = LocalFocusManager.current

                    SearchBarInput(
                        placeholderText = searchBarHint,
                        text = searchQuery,
                        onTextTyped = onSearchQueryChanged,
                        leadingIcon = {
                            AnimatedContent(isTopBarVisible) { isVisible ->
                                if (isVisible) {
                                    IconButton(onClick = { }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_search_icon),
                                            contentDescription = stringResource(R.string.content_description_conversation_search_icon),
                                            tint = MaterialTheme.wireColorScheme.onBackground
                                        )
                                    }
                                } else {
                                    IconButton(onClick = {
                                        focusManager.clearFocus()
                                        showTopBar()

                                        onCloseSearchClicked()
                                    }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_arrow_left),
                                            contentDescription = stringResource(R.string.content_description_conversation_search_icon),
                                            tint = MaterialTheme.wireColorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        },
                        placeholderTextStyle = if (isTopBarVisible) LocalTextStyle.current.copy(textAlign = TextAlign.Center) else LocalTextStyle.current.copy(
                            textAlign = TextAlign.Start
                        ),
                        textStyle = if (isTopBarVisible) LocalTextStyle.current.copy(textAlign = TextAlign.Center) else LocalTextStyle.current.copy(
                            textAlign = TextAlign.Start
                        ),
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    )
                }

                if (interactionSource.collectIsPressedAsState().value) {
                    hideTopBar()
                    onInputClicked()
                }
            }

            AnimatedVisibility(
                visible = isTopBarVisible,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                WireCenterAlignedTopAppBar(
                    elevation = 0.dp,
                    title = topBarTitle,
                    navigationIconType = navigationIconType,
                    onNavigationPressed = { onNavigateBackClicked() }
                )
            }
        }
    }
}

@Composable
private fun rememberSearchbarState(scrollPosition: Int): SearchBarState {
    var isCollapsed by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(scrollPosition) {
        snapshotFlow { scrollPosition }
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

    val searchFieldFullHeightPx: Float =
        LocalDensity.current.run {
            (dimensions().smallTopBarHeight).toPx()
        }

    return remember(isCollapsed) {
        SearchBarState(
            searchFieldFullHeightPx,
            isCollapsed,
        )
    }
}

class SearchBarState(
    private val searchFieldFullHeightPx: Float,
    val isCollapsed: Boolean,
) {

    var isTopBarVisible by mutableStateOf(true)
        private set

    val size
        @Composable get() =
            remember(isTopBarVisible, isCollapsed) {
                if (isCollapsed) {
                    0f
                } else {
                    if (isTopBarVisible) {
                        searchFieldFullHeightPx
                    } else {
                        searchFieldFullHeightPx / 2
                    }
                }
            }

    fun hideTopBar() {
        isTopBarVisible = false
    }

    fun showTopBar() {
        isTopBarVisible = true
    }

}

