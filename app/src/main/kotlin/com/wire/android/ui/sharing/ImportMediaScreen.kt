package com.wire.android.ui.sharing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.util.extension.getActivity

@Composable
fun ImportMediaScreen(
    importMediaViewModel: ImportMediaViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    ImportMediaScreen(importMediaState = importMediaViewModel.importMediaState, onBackPressed = importMediaViewModel::navigateBack)
    context.getActivity()?.let { importMediaViewModel.handleReceivedDataFromSharingIntent(it) }
    // todo : as the screen been recomposed twice this is been triggered twice, need to find a way to trigger it only once
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun ImportMediaScreen(importMediaState: ImportMediaState, onBackPressed: () -> Unit) {
    Scaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = 0.dp,
            onNavigationPressed = onBackPressed,
            title = stringResource(id = R.string.import_media_content_title),
            actions = {
                UserProfileAvatar(
                    avatarData = UserAvatarData(importMediaState.avatarAsset),
                    clickable = remember { Clickable(enabled = true) { } }
                )
            },
        )
    }, modifier = Modifier.background(colorsScheme().background)) { internalPadding ->
        val importedItemsList: List<ImportedMediaAsset> = importMediaState.importedAssets
        val pagerState = rememberPagerState()
        Column(
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState, count = importedItemsList.size, modifier = Modifier.fillMaxWidth()
            ) { page ->
                ImportedMediaItemView(importedItemsList[page])
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewImportMediaScreen() {
    ImportMediaScreen(hiltViewModel())
}
