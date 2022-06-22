package com.wire.android.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry

open class SavedStateViewModel(open val savedStateHandle: SavedStateHandle) : ViewModel()

@Composable
inline fun <reified T : SavedStateViewModel> hiltSavedStateViewModel(navBackStackEntry: NavBackStackEntry? = null) =
    hiltViewModel<T>().apply {
        navBackStackEntry?.let {
            savedStateHandle[EXTRA_BACK_NAVIGATION_ARGUMENTS] = it.savedStateHandle.get<Map<String, Any>>(EXTRA_BACK_NAVIGATION_ARGUMENTS)
        }
    }
