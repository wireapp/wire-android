package com.wire.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorContent

@Composable
fun VoyagerNavigationGraph(startScreens: List<VoyagerNavigationItem>, content: NavigatorContent) {
    Navigator(
        screens = startScreens,
        content = content
    )
}
