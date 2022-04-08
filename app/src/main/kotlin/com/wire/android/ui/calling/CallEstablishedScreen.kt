package com.wire.android.ui.calling

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar


@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CallEstablishedScreen(callEstablishedViewModel: CallEstablishedViewModel = hiltViewModel()) {
    CallEstablishedContent(callEstablishedViewModel.callEstablishedState)
}

@Composable
private fun CallEstablishedContent(state: CallEstablishedState) {
    CallEstablishedTopBar(state.conversationName) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallEstablishedTopBar(
    conversationName: String,
    onCollapse: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCollapse,
        title = conversationName,
        navigationIconType = NavigationIconType.Collapse,
        elevation = 0.dp,
        actions = {}
    )
}

@Preview
@Composable
fun ComposablePreview() {
    CallEstablishedTopBar("Default", {})
}
