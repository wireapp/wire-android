package com.wire.android.ui.home.newconversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.wire.android.R
import com.wire.android.ui.common.NavigableSearchBar
import com.wire.android.ui.common.NavigationIconType
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

@Composable
fun SearchTopBar(
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


//this widget will collapse searchbar on scroll
//this widget will close the topbar when clicked into the inputfield and align the text to the left
@Composable
fun ClosableSearchTopBar(
    scrollPosition: Int,
    content: @Composable () -> Unit,
) {
    ConstraintLayout(Modifier.fillMaxSize()) {
        val (topBarRef, contentRef) = createRefs()

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

        val searchFieldFullHeightPx = LocalDensity.current.run {
            (dimensions().topBarSearchFieldHeight).toPx()
        }

        var isTopBarVisible by remember {
            mutableStateOf(true)
        }

        val size = if (isCollapsed) {
            0f
        } else {

            if (isTopBarVisible) {
                searchFieldFullHeightPx
            } else {
                searchFieldFullHeightPx / 2
            }
        }

        val searchFieldPosition by animateFloatAsState(size)

        Box(modifier = Modifier
            .constrainAs(topBarRef) {
                top.linkTo(parent.top)
                bottom.linkTo(contentRef.top)
            }
            .wrapContentSize()) {
            ClosableSearchBar(searchFieldPosition, { isTopBarVisible = !isTopBarVisible }, isTopBarVisible)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentSize()
                .constrainAs(contentRef) {
                    top.linkTo(topBarRef.bottom)
                    bottom.linkTo(parent.bottom)

                    height = Dimension.fillToConstraints
                }
        ) {
            content()
        }
    }
}


@Composable
fun ClosableSearchBar(searchFieldPosition: Float, onInputPressed: () -> Unit, isTopBarVisible: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
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
            ) {
                NavigableSearchBar(
                    placeholderText = "Search people",
                    onNavigateBack = { },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                )
            }

            if (interactionSource.collectIsPressedAsState().value) {
                onInputPressed()
            }
        }

        AnimatedVisibility(visible = isTopBarVisible) {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(R.string.label_new_conversation),
                navigationIconType = NavigationIconType.Close,
                onNavigationPressed = { }
            )
        }
    }
}
