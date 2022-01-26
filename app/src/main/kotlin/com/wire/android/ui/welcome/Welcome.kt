package com.wire.android.ui.welcome

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import kotlinx.coroutines.delay
import android.content.res.TypedArray
import android.widget.Toast
import androidx.annotation.ArrayRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
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
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.wire.android.ui.theme.body02
import com.wire.android.ui.theme.button02
import com.wire.android.ui.theme.title01

@Preview
@Composable
fun WelcomeScreen() {
    WelcomeContent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeContent() {
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
            WelcomeButtons(modifier = Modifier.padding(top = 40.dp, bottom = 52.dp, start = 16.dp, end = 16.dp))
            WelcomeFooter(modifier = Modifier.padding(bottom = 56.dp, start = 16.dp, end = 16.dp))
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
private fun WelcomeButtons(modifier: Modifier) {
    Column(modifier = modifier) {
        val context = LocalContext.current

        Button(
            shape = RoundedCornerShape(16.dp),
            onClick = { Toast.makeText(context, "Login click 💥", Toast.LENGTH_SHORT).show() }, //TODO
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.label_login),
                style = MaterialTheme.typography.button02
            )
        }

        OutlinedButton(
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
            onClick = { Toast.makeText(context, "Create account click 💥", Toast.LENGTH_SHORT).show() }, //TODO
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.welcome_button_create_enterprise_account),
                style = MaterialTheme.typography.button02.copy(color = MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
private fun WelcomeFooter(modifier: Modifier) {
    Column(modifier = modifier) {
        val context = LocalContext.current

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
                    onClick = { Toast.makeText(context, "Link click 💥", Toast.LENGTH_SHORT).show() } //TODO
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
