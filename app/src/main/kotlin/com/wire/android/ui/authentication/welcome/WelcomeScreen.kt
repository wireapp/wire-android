/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

@file:Suppress("TooManyFunctions")

package com.wire.android.ui.authentication.welcome

import com.wire.android.navigation.annotation.app.WireRootDestination
import android.content.res.TypedArray
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.BuildConfig.ENABLE_NEW_REGISTRATION
import com.wire.android.R
import com.wire.android.config.LocalCustomUiConfigurationProvider
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogContent
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogState
import com.wire.android.ui.common.dialogs.MaxAccountsReachedDialog
import com.wire.android.ui.common.dialogs.MaxAccountsReachedDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.ramcosta.composedestinations.generated.app.destinations.CreateAccountDataDetailScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.CreatePersonalAccountOverviewScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.CreateTeamAccountOverviewScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.LoginScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan

@WireRootDestination(
    style = PopUpNavigationAnimation::class,
    navArgs = WelcomeNavArgs::class
)
@Composable
fun WelcomeScreen(
    navigator: Navigator,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    WelcomeContent(
        viewModel.state.isThereActiveSession,
        viewModel.state.maxAccountsReached,
        viewModel.state.links,
        navigator::navigateBack,
        navigator::navigate
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WelcomeContent(
    isThereActiveSession: Boolean,
    maxAccountsReached: Boolean,
    state: ServerConfig.Links,
    navigateBack: () -> Unit,
    navigate: (NavigationCommand) -> Unit
) {
    val teamCreationUrl = state.teams + stringResource(R.string.create_account_email_backlink_to_team_suffix_url)
    val enterpriseDisabledWithProxyDialogState = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    val createPersonalAccountDisabledWithProxyDialogState = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    val context = LocalContext.current
    WireScaffold(topBar = {
        if (isThereActiveSession) {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                title = "",
                navigationIconType = NavigationIconType.Close(R.string.content_description_welcome_screen_close_btn),
                onNavigationPressed = navigateBack
            )
        } else {
            Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.welcomeVerticalPadding))
        }
    }) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(internalPadding)
        ) {
            val maxAccountsReachedDialogState = rememberVisibilityState<MaxAccountsReachedDialogState>()
            MaxAccountsReachedDialog(dialogState = maxAccountsReachedDialogState) { navigateBack() }
            if (maxAccountsReached) {
                maxAccountsReachedDialogState.show(maxAccountsReachedDialogState.savedState ?: MaxAccountsReachedDialogState)
            }

            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_wire_logo),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = null
            )

            if (state.isOnPremises) {
                ServerTitle(
                    serverLinks = state,
                    modifier = Modifier
                        .padding(top = dimensions().spacing16x, start = dimensions().spacing32x, end = dimensions().spacing32x)
                )
            }

            WelcomeCarousel(modifier = Modifier.weight(1f, true))

            Column(
                modifier = Modifier
                    .padding(
                        vertical = MaterialTheme.wireDimensions.welcomeVerticalSpacing,
                        horizontal = MaterialTheme.wireDimensions.welcomeButtonHorizontalPadding
                    )
                    .semantics {
                        testTagsAsResourceId = true
                    }
            ) {
                LoginButton(
                    onClick = { navigate(NavigationCommand(LoginScreenDestination(loginPasswordPath = LoginPasswordPath(state)))) }
                )
                FeatureDisabledWithProxyDialogContent(
                    dialogState = enterpriseDisabledWithProxyDialogState,
                    onActionButtonClicked = {
                        CustomTabsHelper.launchUrl(context, state.teams)
                    }
                )
                FeatureDisabledWithProxyDialogContent(dialogState = createPersonalAccountDisabledWithProxyDialogState)

                if (LocalCustomUiConfigurationProvider.current.isAccountCreationAllowed) {
                    CreateEnterpriseAccountButton {
                        if (state.isProxyEnabled()) {
                            enterpriseDisabledWithProxyDialogState.show(
                                enterpriseDisabledWithProxyDialogState.savedState ?: FeatureDisabledWithProxyDialogState(
                                    R.string.create_team_not_supported_dialog_description,
                                    state.teams
                                )
                            )
                        } else {
                            if (ENABLE_NEW_REGISTRATION) {
                                CustomTabsHelper.launchUrl(context, teamCreationUrl)
                            } else {
                                navigate(NavigationCommand(CreateTeamAccountOverviewScreenDestination(state)))
                            }
                        }
                    }
                }
            }

            if (LocalCustomUiConfigurationProvider.current.isAccountCreationAllowed) {
                WelcomeFooter(
                    modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding),
                    onPrivateAccountClick = {
                        if (state.isProxyEnabled()) {
                            createPersonalAccountDisabledWithProxyDialogState.show(
                                createPersonalAccountDisabledWithProxyDialogState.savedState ?: FeatureDisabledWithProxyDialogState(
                                    R.string.create_personal_account_not_supported_dialog_description
                                )
                            )
                        } else {
                            if (ENABLE_NEW_REGISTRATION) {
                                navigate(
                                    NavigationCommand(
                                        CreateAccountDataDetailScreenDestination(
                                            CreateAccountDataNavArgs(
                                                customServerConfig = state
                                            )
                                        )
                                    )
                                )
                            } else {
                                navigate(NavigationCommand(CreatePersonalAccountOverviewScreenDestination(state)))
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WelcomeCarousel(modifier: Modifier = Modifier) {
    val delay = integerResource(id = R.integer.welcome_carousel_item_time_ms)
    val icons: List<Int> = typedArrayResource(id = R.array.welcome_carousel_icons).drawableResIdList()
    val texts: List<String> = stringArrayResource(id = R.array.welcome_carousel_texts).toList()
    val items: List<CarouselPageData> = icons.zip(texts) { icon, text -> CarouselPageData(icon, text) }

    // adding repeated elements on both edges to have list like: [E A B C D E A] and because of that we can flip to the other side of the
    // list when we reach the end while keeping swipe capability both ways and from the user side it looks like an infinite loop both ways
    val circularItemsList = listOf<CarouselPageData>().plus(items.last()).plus(items).plus(items.first())
    val initialPage = 1
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { circularItemsList.size })

    LaunchedEffect(pagerState) {
        autoScrollCarousel(pagerState, initialPage, circularItemsList, delay.toLong())
    }

    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxWidth()
        ) { page ->
            val (pageIconResId, pageText) = circularItemsList[page]
            WelcomeCarouselItem(pageIconResId = pageIconResId, pageText = pageText)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalFoundationApi::class)
private suspend fun autoScrollCarousel(
    pageState: PagerState,
    initialPage: Int,
    circularItemsList: List<CarouselPageData>,
    delay: Long
) = snapshotFlow { pageState.currentPage }.distinctUntilChanged()
    .scan(initialPage to initialPage) { (_, previousPage), currentPage -> previousPage to currentPage }
    .flatMapLatest { (previousPage, currentPage) ->
        when {
            shouldJumpToStart(previousPage, currentPage, circularItemsList.lastIndex, initialPage) -> flow {
                emit(
                    CarouselScrollData(
                        scrollToPage = initialPage,
                        animate = false
                    )
                )
            }

            shouldJumpToEnd(
                previousPage,
                currentPage,
                circularItemsList.lastIndex
            ) -> flow { emit(CarouselScrollData(scrollToPage = circularItemsList.lastIndex - 1, animate = false)) }

            else -> flow { emit(CarouselScrollData(scrollToPage = pageState.currentPage + 1, animate = true)) }.onEach {
                delay(
                    delay
                )
            }
        }
    }.collect { (scrollToPage, animate) ->
        if (pageState.pageCount != 0) {
            if (animate) {
                pageState.animateScrollToPage(scrollToPage)
            } else {
                pageState.scrollToPage(scrollToPage)
            }
        }
    }

@Composable
private fun WelcomeCarouselItem(pageIconResId: Int, pageText: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = pageIconResId),
            contentDescription = null,
            contentScale = ContentScale.Inside,
            modifier = Modifier
                .weight(1f, true)
                .padding(
                    horizontal = MaterialTheme.wireDimensions.welcomeImageHorizontalPadding,
                    vertical = MaterialTheme.wireDimensions.welcomeVerticalSpacing
                )
        )
        Text(
            text = pageText,
            style = MaterialTheme.wireTypography.title01,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding)
                .clearAndSetSemantics {}
        )
    }
}

@Composable
private fun LoginButton(onClick: () -> Unit) {
    WirePrimaryButton(
        onClick = onClick,
        text = stringResource(R.string.label_login),
        modifier = Modifier
            .padding(bottom = MaterialTheme.wireDimensions.welcomeButtonVerticalPadding)
            .testTag("loginButton")
    )
}

@Composable
private fun CreateEnterpriseAccountButton(onClick: () -> Unit) {
    WireSecondaryButton(
        onClick = onClick,
        text = stringResource(R.string.welcome_button_create_team),
        modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.welcomeButtonVerticalPadding)
    )
}

@Composable
private fun WelcomeFooter(onPrivateAccountClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.welcome_footer_text),
            style = MaterialTheme.wireTypography.body02,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.welcome_button_create_personal_account),
            style = MaterialTheme.wireTypography.body02.copy(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPrivateAccountClick,
                    onClickLabel = stringResource(R.string.content_description_open_link_label)
                )
        )

        Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.welcomeVerticalPadding))
    }
}

@Composable
@ReadOnlyComposable
private fun typedArrayResource(@ArrayRes id: Int): TypedArray = LocalContext.current.resources.obtainTypedArray(id)

private fun TypedArray.drawableResIdList(): List<Int> = (0 until this.length()).map { this.getResourceId(it, 0) }

// having list [E A B C D E A], when moving forward we reach the last one - second "A", we want to flip to the first "A"
// to keep swipe capability both ways and the feeling of an endless loop
private fun shouldJumpToStart(previousPage: Int, currentPage: Int, lastPage: Int, initialPage: Int): Boolean =
    currentPage == lastPage && previousPage < currentPage && previousPage >= initialPage

// having list [E A B C D E A], when moving backward we reach the first one - first "E", we want to flip to the second "E"
// to keep swipe capability both ways and the feeling of an endless loop
private fun shouldJumpToEnd(previousPage: Int, currentPage: Int, lastPage: Int): Boolean =
    currentPage == 0 && previousPage > currentPage && previousPage < lastPage

@Preview
@Composable
fun PreviewWelcomeScreen() {
    WireTheme {
        WelcomeContent(
            isThereActiveSession = false,
            maxAccountsReached = false,
            state = ServerConfig.DEFAULT,
            navigateBack = {},
            navigate = {}
        )
    }
}

private data class CarouselScrollData(val scrollToPage: Int, val animate: Boolean)
private data class CarouselPageData(@DrawableRes val icon: Int, val text: String)
