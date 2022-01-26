package com.wire.android.ui.topbar

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.NavigationType
import com.wire.android.navigation.TopBarBtn
import com.wire.android.ui.common.SearchBarUI
import com.wire.android.ui.common.UserProfileAvatar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun WireTopBar(
    navigationType: NavigationType?,
    viewModel: TopBarViewModel = hiltViewModel()
) {
    if (navigationType is NavigationType.WithTopBar) {
        ToolBarWithBtn(navigationType, viewModel)
    }
}

@Composable
private fun ToolBarWithBtn(
    data: NavigationType.WithTopBar,
    viewModel: TopBarViewModel
) {
    val title = stringResource(id = data.title)
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
    ) {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            elevation = 0.dp,
            backgroundColor = MaterialTheme.colors.background,
            contentColor = MaterialTheme.colors.onBackground,
            content = {
                ToolbarIconBtn(data, viewModel, scope)
                Text(modifier = Modifier.weight(weight = 1f), textAlign = TextAlign.Center, text = title, fontSize = 18.sp)

                if (data.hasUserAvatar) {
                    UserProfileAvatar(avatarUrl = "") {
                        println("cyka 0")
                        scope.launch { viewModel.openUserProfile() }
                    }
                }
            },
        )
        if (data.isSearchable) {
            SearchBarUI(placeholderText = stringResource(R.string.search_bar_hint, title.lowercase()))
        }
    }
}

@Composable
private fun ToolbarIconBtn(
    navigationType: NavigationType.WithTopBar,
    viewModel: TopBarViewModel,
    scope: CoroutineScope
) {
    val imageVector: ImageVector
    val contentDescription: String
    val onClick: () -> Unit

    when (navigationType.btnType) {
        TopBarBtn.MENU -> {
            imageVector = Icons.Filled.Menu
            contentDescription = "Open Drawer"
            onClick = { scope.launch { viewModel.openDrawer() } }
        }
        TopBarBtn.BACK -> {
            imageVector = Icons.Filled.ArrowBack
            contentDescription = "Go back"
            onClick = { scope.launch { viewModel.goBack() } }
        }
        TopBarBtn.CLOSE -> {
            imageVector = Icons.Filled.Close
            contentDescription = "Close"
            onClick = { scope.launch { viewModel.goBack() } }
        }
    }

    IconButton(onClick = onClick) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = MaterialTheme.colors.onBackground
        )
    }
}

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Preview(showBackground = false)
@Composable
fun WireTopBarWithBurgerPreview() {
    WireTopBar(
        navigationType = NavigationType.WithTopBar.WithDrawer(
            title = R.string.conversations_screen_title,
            isSearchable = true,
            hasUserAvatar = true
        ) as NavigationType
    )
}
