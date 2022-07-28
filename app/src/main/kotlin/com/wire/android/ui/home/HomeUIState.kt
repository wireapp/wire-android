@file:OptIn(ExperimentalAnimationApi::class)

package com.wire.android.ui.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
class HomeUIState(
    val coroutineScope: CoroutineScope,
    val drawerState: DrawerState,
    val bottomSheetState: ModalBottomSheetState
) {
    var currentNavigationItem: HomeItem by mutableStateOf(HomeItem.all.first())

    var scrollPositionProvider: (() -> Int)? by mutableStateOf(null)

    var homeBottomSheetContent: @Composable (ColumnScope.() -> Unit)? by mutableStateOf(null)
        private set

    fun toggleBottomSheetVisibility() {
        coroutineScope.launch {
            if (bottomSheetState.isVisible) bottomSheetState.animateTo(ModalBottomSheetValue.Hidden)
            else bottomSheetState.animateTo(ModalBottomSheetValue.Expanded)
        }
    }

    fun changeBottomSheetContent(content: @Composable ColumnScope.() -> Unit) {
        homeBottomSheetContent = content
    }

    fun updateScrollPositionProvider(newScrollPositionProvider: () -> Int) {
        scrollPositionProvider = newScrollPositionProvider
    }

    fun openDrawer() {
        coroutineScope.launch {
            drawerState.open()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberHomeUIState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
): HomeUIState = remember { HomeUIState(coroutineScope, drawerState, bottomSheetState) }
