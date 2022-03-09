package com.wire.android.ui.common.topappbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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


/**
 * AppTopBarWithSearchBarLayout is a "wrapper" around the [content] that should have a TopBar with a SearchBar
 * on the Top of the [content]. To collapse the searchbar when the user scrolls down on the list within the [content]
 * it is necessary to pass a [scrollPosition] usually it is a firstVisibleItemIndex coming out of the list within the content.
 * AppTopBarWithSearchBarLayout also exposes [searchQuery] as well as [onSearchQueryChanged] so that we are able to manipulate
 * the [searchQuery] outside of the AppTopBarWithSearchBarLayout for example a ViewModel. Beside collapsing the searchbar when scrolling
 * through the list, AppTopBarWithSearchBarLayout also hides/shows the TopBar whenever the SearchBarInput is clicked so that we
 * can transit to the state where we provide the search query to the searchbar.
 */
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
        isSearchBarCollapsed = searchBarState.isSearchBarCollapsed,
        isTopBarVisible = searchBarState.isTopBarVisible,
        searchBarHint = searchBarHint,
        searchQuery = searchQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        onInputClicked = {
            searchBarState.hideTopBar()

            onSearchClicked()
        },
        onCloseSearchClicked = {
            onCloseSearchClicked()

            searchBarState.showTopBar()
        },
        appTopBar = appTopBar
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AppTopBarWithSearchBarContent(
    isSearchBarCollapsed: Boolean,
    isTopBarVisible: Boolean,
    searchBarHint: String,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onInputClicked: () -> Unit,
    onCloseSearchClicked: () -> Unit,
    appTopBar: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shadowElevation = if (isTopBarVisible) dimensions().spacing8x else 0.dp
    ) {
        Column(
            Modifier
                .wrapContentSize()
                .animateContentSize(animationSpec = snap())
        ) {
            AnimatedVisibility(
                visible = isTopBarVisible,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Box(
                    Modifier
                        .wrapContentSize()
                ) {
                    appTopBar()
                }
            }

            AnimatedVisibility(!isSearchBarCollapsed) {
                Box(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .background(MaterialTheme.wireColorScheme.background)
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
                        modifier = Modifier.padding(8.dp)
                    )

                    if (interactionSource.collectIsPressedAsState().value) {
                        onInputClicked()
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberSearchbarState(scrollPosition: Int): SearchBarState {
    val searchBarState = remember {
        SearchBarState()
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
            .distinctUntilChanged().collect { shouldCollapse ->
                searchBarState.isSearchBarCollapsed = shouldCollapse
            }
    }

    return searchBarState
}

class SearchBarState {

    var isSearchBarCollapsed by mutableStateOf(false)

    var isTopBarVisible by mutableStateOf(true)
        private set

    fun hideTopBar() {
        isTopBarVisible = false
    }

    fun showTopBar() {
        isTopBarVisible = true
    }
}
