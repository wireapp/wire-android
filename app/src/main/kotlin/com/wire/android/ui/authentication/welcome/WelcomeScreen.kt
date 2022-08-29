@file:Suppress("TooManyFunctions")

package com.wire.android.ui.authentication.welcome

import android.content.res.TypedArray
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import java.net.URL

@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel = hiltViewModel()) {
    WelcomeContent(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeContent(viewModel: WelcomeViewModel) {
    Scaffold(topBar = {
        if (viewModel.isThereActiveSession && viewModel.state.isOnPremises) {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = "",
                navigationIconType = NavigationIconType.Close,
                onNavigationPressed = viewModel::navigateBack
            )
        }
    }) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(internalPadding)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_wire_logo),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(id = R.string.content_description_welcome_wire_logo)
            )

            if (viewModel.state.isOnPremises) {
                ServerTitle(serverLinks = viewModel.state, modifier = Modifier.padding(top = dimensions().spacing16x))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f, true)
            ) {
                WelcomeCarousel()
            }

            Column(
                modifier = Modifier.padding(
                    vertical = MaterialTheme.wireDimensions.welcomeVerticalSpacing,
                    horizontal = MaterialTheme.wireDimensions.welcomeButtonHorizontalPadding
                )
            ) {
                LoginButton {
                    viewModel.goToLogin()
                }
                CreateEnterpriseAccountButton {
                    viewModel.goToCreateEnterpriseAccount()
                }
            }

            WelcomeFooter(modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding),
                onPrivateAccountClick = {
                    viewModel.goToCreatePrivateAccount()
                })
        }

    }
}

@OptIn(ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
private fun WelcomeCarousel() {
    val delay = integerResource(id = R.integer.welcome_carousel_item_time_ms)
    val icons: List<Int> = typedArrayResource(id = R.array.welcome_carousel_icons).drawableResIdList()
    val texts: List<String> = stringArrayResource(id = R.array.welcome_carousel_texts).toList()
    val items: List<CarouselPageData> = icons.zip(texts) { icon, text -> CarouselPageData(icon, text) }

    // adding repeated elements on both edges to have list like: [E A B C D E A] and because of that we can flip to the other side of the
    // list when we reach the end while keeping swipe capability both ways and from the user side it looks like an infinite loop both ways
    val circularItemsList = listOf<CarouselPageData>().plus(items.last()).plus(items).plus(items.first())
    val initialPage = 1
    val pagerState = rememberPagerState(initialPage = initialPage)

    LaunchedEffect(pagerState) {
        autoScrollCarousel(pagerState, initialPage, circularItemsList, delay.toLong())
    }

    CompositionLocalProvider(LocalOverScrollConfiguration provides null) {
        HorizontalPager(
            state = pagerState, count = circularItemsList.size, modifier = Modifier.fillMaxWidth()
        ) { page ->
            val (pageIconResId, pageText) = circularItemsList[page]
            WelcomeCarouselItem(pageIconResId = pageIconResId, pageText = pageText)
        }
    }
}

@OptIn(ExperimentalPagerApi::class, ExperimentalCoroutinesApi::class)
private suspend fun autoScrollCarousel(
    pageState: PagerState, initialPage: Int, circularItemsList: List<CarouselPageData>, delay: Long
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

            else -> flow { emit(CarouselScrollData(scrollToPage = pageState.currentPage + 1, animate = true)) }.onEach { delay(delay) }
        }
    }.collect { (scrollToPage, animate) ->
        if (pageState.pageCount != 0) {
            if (animate) pageState.animateScrollToPage(scrollToPage)
            else pageState.scrollToPage(scrollToPage)
        }
    }

@Composable
private fun WelcomeCarouselItem(pageIconResId: Int, pageText: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = pageIconResId),
            contentDescription = "",
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
            modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding)
        )
    }
}

@Composable
private fun LoginButton(onClick: () -> Unit) {
    WirePrimaryButton(
        onClick = onClick,
        text = stringResource(R.string.label_login),
        modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.welcomeButtonVerticalPadding)
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
private fun WelcomeFooter(modifier: Modifier, onPrivateAccountClick: () -> Unit) {
    Column(modifier = modifier) {

        Text(
            text = stringResource(R.string.welcome_footer_text),
            style = MaterialTheme.wireTypography.body02,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.welcome_button_create_personal_account), style = MaterialTheme.wireTypography.body02.copy(
                textDecoration = TextDecoration.Underline, color = MaterialTheme.colorScheme.primary
            ), textAlign = TextAlign.Center, modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onPrivateAccountClick
                )
        )
    }
}

@Composable
private fun ServerTitle(serverLinks: ServerConfig.Links, modifier: Modifier = Modifier) {
    ConstraintLayout(
        modifier = Modifier
            .padding(horizontal = dimensions().spacing32x)
            .fillMaxWidth()
            .then(modifier)
    ) {
        val (serverTitle, infoIcon) = createRefs()

        var serverFullDetailsDialogState: Boolean by remember { mutableStateOf(false) }

        Text(
            modifier = Modifier.constrainAs(serverTitle) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            text = URL(serverLinks.api).host,
            style = MaterialTheme.wireTypography.title01,
            color = MaterialTheme.wireColorScheme.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Icon(painter = painterResource(id = R.drawable.ic_info),
            contentDescription = null,
            modifier = Modifier
                .constrainAs(infoIcon) {
                    start.linkTo(serverTitle.end)
                    centerVerticallyTo(serverTitle)
                }
                .padding(start = dimensions().spacing8x)
                .size(MaterialTheme.wireDimensions.wireIconButtonSize)
                .clickable(Clickable(true, onClick = { serverFullDetailsDialogState = true })),
            tint = MaterialTheme.wireColorScheme.secondaryText
        )

        if (serverFullDetailsDialogState) {
            WireDialog(
                title = stringResource(id = R.string.server_details_dialog_title),
                text = LocalContext.current.resources.stringWithStyledArgs(
                    R.string.server_details_dialog_body,
                    MaterialTheme.wireTypography.body02,
                    MaterialTheme.wireTypography.body02,
                    normalColor = colorsScheme().secondaryText,
                    argsColor = colorsScheme().onBackground,
                    serverLinks.title,
                    serverLinks.api
                ),
                onDismiss = { serverFullDetailsDialogState = false },
                optionButton1Properties = WireDialogButtonProperties(
                    stringResource(id = R.string.label_ok),
                    onClick = { serverFullDetailsDialogState = false },
                    type = WireDialogButtonType.Primary
                )
            )
        }
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
private fun WelcomeScreenPreview() {
    WireTheme(isPreview = true) {
        WelcomeContent(hiltViewModel())
    }
}

private data class CarouselScrollData(val scrollToPage: Int, val animate: Boolean)
private data class CarouselPageData(@DrawableRes val icon: Int, val text: String)

