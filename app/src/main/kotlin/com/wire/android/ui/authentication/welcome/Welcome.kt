package com.wire.android.ui.authentication.welcome

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import kotlinx.coroutines.delay
import android.content.res.TypedArray
import androidx.annotation.ArrayRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.wire.android.ui.authentication.AuthDestination
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.theme.body02
import com.wire.android.ui.theme.title01

//@Preview
@Composable
fun WelcomeScreen(navController: NavController) {
    WelcomeContent(navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeContent(navController: NavController) {
    Scaffold {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_wire_logo),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(id = R.string.welcome_wire_logo_content_description),
                modifier = Modifier.padding(48.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f, true)
            ) {
                WelcomeCarousel()
            }

            Column(modifier = Modifier.padding(top = 40.dp, bottom = 52.dp, start = 16.dp, end = 16.dp),) {
                LoginButton {
                    navController.navigate(AuthDestination.loginScreen)
                }
                CreateEnterpriseAccountButton {
                    navController.navigate((AuthDestination.createEnterpriseAccount))

                }
            }

            WelcomeFooter(modifier = Modifier.padding(bottom = 56.dp, start = 16.dp, end = 16.dp),
                onPrivateAccountClick = {
                    navController.navigate(AuthDestination.createPrivateAccountScreen)
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
    val items: List<Pair<Int, String>> = icons zip texts
    val circularItemsList = listOf<Pair<Int, String>>().plus(items.last()).plus(items)
    val pageState = rememberPagerState(initialPage = 1)

    LaunchedEffect(pageState.currentPage) {
        if (pageState.currentPage == circularItemsList.lastIndex) {
            pageState.scrollToPage(0)
        } else {
            delay(delay.toLong())
            pageState.animateScrollToPage(pageState.currentPage + 1)
        }
    }

    CompositionLocalProvider(LocalOverScrollConfiguration provides null) {
        HorizontalPager(
            state = pageState,
            count = circularItemsList.size,
            modifier = Modifier
                .fillMaxWidth()
                .disablePointerInputScroll()
        ) { page ->
            val (pageIconResId, pageText) = circularItemsList[page]
            WelcomeCarouselItem(pageIconResId = pageIconResId, pageText = pageText)
        }
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
                .padding(start = 64.dp, end = 64.dp, bottom = 36.dp)
        )
        Text(
            text = pageText,
            style = MaterialTheme.typography.title01,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun LoginButton(onClick: () -> Unit) {
    WirePrimaryButton(
        onClick = onClick,
        text = stringResource(R.string.label_login),
        modifier = Modifier
            .fillMaxWidth()
    )
}

@Composable
private fun CreateEnterpriseAccountButton(onClick: () -> Unit) {
    WireSecondaryButton(
        onClick = onClick,
        text = stringResource(R.string.welcome_button_create_enterprise_account),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
}

@Composable
private fun WelcomeFooter(modifier: Modifier, onPrivateAccountClick: () -> Unit) {
    Column(modifier = modifier) {

        Text(
            text = stringResource(R.string.welcome_footer_text),
            style = MaterialTheme.typography.body02,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.welcome_footer_link),
            style = MaterialTheme.typography.body02.copy(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPrivateAccountClick
                )
        )
    }
}

@Composable
@ReadOnlyComposable
private fun typedArrayResource(@ArrayRes id: Int): TypedArray = LocalContext.current.resources.obtainTypedArray(id)

private fun TypedArray.drawableResIdList(): List<Int> = (0 until this.length()).map { this.getResourceId(it, 0) }

private fun Modifier.disablePointerInputScroll() = this.nestedScroll(object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource) = available
})
