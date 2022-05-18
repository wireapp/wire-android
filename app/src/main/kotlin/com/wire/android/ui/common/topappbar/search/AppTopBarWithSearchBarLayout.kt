package com.wire.android.ui.common.topappbar.search

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.wire.android.R
import com.wire.android.ui.common.SearchBarInput
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme

/**
 * AppTopBarWithSearchBarLayout is a "wrapper" around the [content] that should have a TopBar with a SearchBar
 * on the Top of the [content]. To collapse the searchbar when the user scrolls down on the list within the [content]
 * it is necessary to pass a [scrollPosition] usually it is a firstVisibleItemIndex coming out of the list within the content.
 * AppTopBarWithSearchBarLayout also exposes [searchQuery] as well as [onSearchQueryChanged] so that we are able to manipulate
 * the [searchQuery] outside of the AppTopBarWithSearchBarLayout for example a ViewModel. Beside collapsing the searchbar when scrolling
 * through the list, AppTopBarWithSearchBarLayout also hides/shows the TopBar whenever the SearchBarInput is clicked so that we
 * can transit to the state where we provide the search query to the searchbar.
 */
@Composable
fun AppTopBarWithSearchBar(
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
        isSearchActive = searchBarState.isSearchActive,
        isSearchBarCollapsed = searchBarState.isSearchBarCollapsed,
        searchBarHint = searchBarHint,
        searchQuery = if (searchBarState.isSearchActive) searchQuery else "",
        onSearchQueryChanged = onSearchQueryChanged,
        onInputClicked = {
            searchBarState.startSearch()

            onSearchClicked()
        },
        onCloseSearchClicked = {
            onCloseSearchClicked()
            searchBarState.cancelSearch()
        },
        appTopBar = appTopBar
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AppTopBarWithSearchBarContent(
    isSearchActive: Boolean,
    isSearchBarCollapsed: Boolean,
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
        shadowElevation = if (!isSearchActive) dimensions().spacing8x else 0.dp
    ) {
        Column(
            Modifier
                .wrapContentSize()
                .animateContentSize(animationSpec = snap())
        ) {
            AnimatedVisibility(
                visible = !isSearchActive,
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
                            AnimatedContent(!isSearchActive) { isVisible ->
                                IconButton(onClick = {
                                    if (!isVisible) {
                                        focusManager.clearFocus()

                                        onCloseSearchClicked()
                                    }
                                }) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (isVisible) R.drawable.ic_search
                                            else R.drawable.ic_arrow_left
                                        ),
                                        contentDescription = stringResource(R.string.content_description_conversation_search_icon),
                                        tint = MaterialTheme.wireColorScheme.onBackground
                                    )
                                }
                            }
                        },
                        placeholderTextStyle = textStyleAlignment(isTopBarVisible = !isSearchActive),
                        textStyle = textStyleAlignment(isTopBarVisible = !isSearchActive),
                        interactionSource = interactionSource,
                        modifier = Modifier.padding(dimensions().spacing8x)
                    )

                    if (interactionSource.collectIsPressedAsState().value) {
                        // we want to propagate the click on the input of the search
                        // only the first time the user clicks on the input
                        // that is when the search is not active
                        if (!isSearchActive) {
                            onInputClicked()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun textStyleAlignment(isTopBarVisible: Boolean): TextStyle {
    return if (isTopBarVisible) LocalTextStyle.current.copy(textAlign = TextAlign.Center) else LocalTextStyle.current.copy(
        textAlign = TextAlign.Start
    )
}

