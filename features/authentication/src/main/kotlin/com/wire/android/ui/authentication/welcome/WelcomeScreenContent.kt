/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.welcome

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan

@Immutable
data class WelcomePresentationState(
    val showCloseButton: Boolean,
    val accountCreationAllowed: Boolean,
    val showTeamCreation: Boolean,
    val showPersonalCreation: Boolean,
    val carouselDelayMillis: Long,
    val carouselPages: List<WelcomeCarouselPage>,
)

@Immutable
data class WelcomeCarouselPage(@DrawableRes val icon: Int, val text: String)

@Composable
fun WelcomeScreenContent(
    state: WelcomePresentationState,
    loginLabel: String,
    createTeamLabel: String,
    footerText: String,
    createPersonalLabel: String,
    openLinkDescription: String,
    @StringRes closeContentDescription: Int,
    onClose: () -> Unit,
    onLogin: () -> Unit,
    onCreateTeam: () -> Unit,
    onCreatePersonal: () -> Unit,
    logoContent: @Composable () -> Unit,
    serverTitleContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bodyOverride: (@Composable ColumnScope.() -> Unit)? = null,
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            if (state.showCloseButton) {
                WireCenterAlignedTopAppBar(
                    elevation = dimensions().spacing0x,
                    title = "",
                    navigationIconType = NavigationIconType.Close(closeContentDescription),
                    onNavigationPressed = onClose,
                )
            } else {
                Spacer(Modifier.height(MaterialTheme.wireDimensions.welcomeVerticalPadding))
            }
        },
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(padding),
        ) {
            if (bodyOverride != null) {
                bodyOverride()
                return@Column
            }
            logoContent()
            serverTitleContent()
            WelcomeCarousel(state.carouselPages, state.carouselDelayMillis, Modifier.weight(1f, true))
            Column(
                Modifier
                    .padding(
                        vertical = MaterialTheme.wireDimensions.welcomeVerticalSpacing,
                        horizontal = MaterialTheme.wireDimensions.welcomeButtonHorizontalPadding,
                    )
                    .semantics { testTagsAsResourceId = true },
            ) {
                WirePrimaryButton(
                    onClick = onLogin,
                    text = loginLabel,
                    modifier = Modifier
                        .padding(bottom = MaterialTheme.wireDimensions.welcomeButtonVerticalPadding)
                        .testTag("loginButton"),
                )
                if (state.accountCreationAllowed && state.showTeamCreation) {
                    WireSecondaryButton(
                        onClick = onCreateTeam,
                        text = createTeamLabel,
                        modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.welcomeButtonVerticalPadding),
                    )
                }
            }
            if (state.accountCreationAllowed && state.showPersonalCreation) {
                WelcomeFooter(footerText, createPersonalLabel, openLinkDescription, onCreatePersonal)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WelcomeCarousel(
    pages: List<WelcomeCarouselPage>,
    delayMillis: Long,
    modifier: Modifier,
) {
    if (pages.isEmpty()) return
    val circular = listOf(pages.last()) + pages + listOf(pages.first())
    val pager = rememberPagerState(initialPage = 1, pageCount = { circular.size })
    LaunchedEffect(pager, delayMillis) {
        autoScrollCarousel(pager, 1, circular.lastIndex, delayMillis)
    }
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        HorizontalPager(state = pager, modifier = modifier.fillMaxWidth()) { page ->
            val item = circular[page]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(item.icon),
                    contentDescription = null,
                    contentScale = ContentScale.Inside,
                    modifier = Modifier
                        .weight(1f, true)
                        .padding(
                            horizontal = MaterialTheme.wireDimensions.welcomeImageHorizontalPadding,
                            vertical = MaterialTheme.wireDimensions.welcomeVerticalSpacing,
                        ),
                )
                Text(
                    item.text,
                    style = MaterialTheme.wireTypography.title01,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding)
                        .clearAndSetSemantics {},
                )
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalFoundationApi::class)
private suspend fun autoScrollCarousel(
    pageState: PagerState,
    initialPage: Int,
    lastPage: Int,
    delayMillis: Long,
) = snapshotFlow { pageState.currentPage }
    .distinctUntilChanged()
    .scan(initialPage to initialPage) { (_, previous), current -> previous to current }
        .flatMapLatest { (previous, current) ->
            when {
                shouldJumpToStart(previous, current, lastPage, initialPage) -> {
                    flow { emit(CarouselScroll(initialPage, false)) }
                }
                shouldJumpToEnd(previous, current, lastPage) -> {
                    flow { emit(CarouselScroll(lastPage - 1, false)) }
                }
                else -> flow { emit(CarouselScroll(pageState.currentPage + 1, true)) }
                    .onEach { delay(delayMillis) }
            }
        }
        .collect { (page, animate) ->
            if (animate) pageState.animateScrollToPage(page) else pageState.scrollToPage(page)
        }

@Composable
private fun WelcomeFooter(
    text: String,
    link: String,
    openLinkDescription: String,
    onClick: () -> Unit,
) =
    Column(Modifier.padding(horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding)) {
        Text(
            text = text,
            style = MaterialTheme.wireTypography.body02,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = link,
            style = MaterialTheme.wireTypography.body02.copy(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colorScheme.primary,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onClickLabel = openLinkDescription,
                ),
        )
        Spacer(Modifier.height(MaterialTheme.wireDimensions.welcomeVerticalPadding))
    }

internal fun shouldJumpToStart(
    previousPage: Int,
    currentPage: Int,
    lastPage: Int,
    initialPage: Int,
): Boolean = currentPage == lastPage && previousPage < currentPage && previousPage >= initialPage

internal fun shouldJumpToEnd(previousPage: Int, currentPage: Int, lastPage: Int): Boolean =
    currentPage == 0 && previousPage > currentPage && previousPage < lastPage
private data class CarouselScroll(val page: Int, val animate: Boolean)
