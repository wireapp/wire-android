package com.wire.android.ui.home.newconversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.wire.android.R
import com.wire.android.ui.common.SearchBarInput
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppTopBarWithSearchBarLayout(
    scrollPosition: Int,
    searchBarHint: String,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onSearchClicked: () -> Unit = {},
    onCloseSearchClicked: () -> Unit = {},
    content: @Composable () -> Unit,
    appTopBar: @Composable () -> Unit,
) {
    ConstraintLayout(
        Modifier
            .fillMaxSize()
    ) {
        val (topBarRef, contentRef) = createRefs()

        Box(
            Modifier
                .wrapContentSize()
                .constrainAs(contentRef) {
                    top.linkTo(topBarRef.bottom)
                    bottom.linkTo(parent.bottom)

                    height = Dimension.fillToConstraints
                }
        ) {
            content()
        }

        Box(
            Modifier
                .wrapContentSize()
                .constrainAs(topBarRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(contentRef.top)
                }
        ) {
            AppTopBarWithSearchBar(
                scrollPosition = scrollPosition,
                searchBarHint = searchBarHint,
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                onSearchClicked = onSearchClicked,
                onCloseSearchClicked = onCloseSearchClicked,
                appTopBar = appTopBar
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AppTopBarWithSearchBar(
    scrollPosition: Int,
    searchBarHint: String,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onSearchClicked: () -> Unit = {},
    onCloseSearchClicked: () -> Unit = {},
    appTopBar: @Composable () -> Unit,
) {
    val searchBarState = rememberSearchbarState(scrollPosition)

    AppTopBarWithSearchBarContent(
        searchBarState = searchBarState,
        searchBarHint = searchBarHint,
        searchQuery = searchQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        onInputClicked = onSearchClicked,
        onCloseSearchClicked = onCloseSearchClicked,
        appTopBar = appTopBar
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AppTopBarWithSearchBarContent(
    searchBarState: SearchBarState,
    searchBarHint: String,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onInputClicked: () -> Unit,
    onCloseSearchClicked: () -> Unit,
    appTopBar: @Composable () -> Unit,
) {
    with(searchBarState) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .animateContentSize(),
            shadowElevation = if (isTopBarVisible) 8.dp else 0.dp
        ) {
            ConstraintLayout(Modifier.wrapContentSize()) {
                val (searchInputRef, topBarRef) = createRefs()

                Box(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .constrainAs(searchInputRef) {
                            if (!isSearchBarCollapsed) {
                                top.linkTo(topBarRef.bottom)
                            } else {
                                top.linkTo(parent.top)
                            }
                        }
                        .background(Color.Green)
                ) {
                    val interactionSource = remember {
                        MutableInteractionSource()
                    }

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
                    )

                    if (interactionSource.collectIsPressedAsState().value) {
                        hideTopBar()

                        onInputClicked()
                    }
                }

                Box(
                    Modifier
                        .wrapContentSize()
                        .constrainAs(topBarRef) {
                            top.linkTo(parent.top)
                        }
                ) {
                    AnimatedVisibility(
                        visible = isTopBarVisible,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        appTopBar()
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberSearchbarState(scrollPosition: Int): SearchBarState {
    val searchFieldFullHeightPx = LocalDensity.current.run {
        (dimensions().topBarSearchFieldHeight).toPx()
    }

    val searchBarState = remember {
        SearchBarState(searchFieldFullHeightPx)
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
                searchBarState.isSearchBarCollapsed = it
            }
    }

    return searchBarState
}

class SearchBarState(
    private val searchFieldFullHeightPx: Float
) {

    var isSearchBarCollapsed by mutableStateOf(false)

    var isTopBarVisible by mutableStateOf(true)
        private set

    val size
        @Composable get() =
            remember(isTopBarVisible, isSearchBarCollapsed) {
                if (isSearchBarCollapsed) {
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
