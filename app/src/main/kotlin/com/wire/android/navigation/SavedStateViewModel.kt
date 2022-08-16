package com.wire.android.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry

/*
 * ViewModel extension which already has SavedStateHandle field.
 * It is recommended to use it for NavigationItem together with the hiltSavedStateViewModel method which requires a NavBackStackEntry
 * in order to be able to receive arguments when navigating back using popWithArguments.
 */
open class SavedStateViewModel(open val savedStateHandle: SavedStateHandle) : ViewModel()

@Composable
inline fun <reified T : SavedStateViewModel> hiltSavedStateViewModel(navBackStackEntry: NavBackStackEntry) =
    hiltViewModel<T>().apply {
        savedStateHandle[EXTRA_BACK_NAVIGATION_ARGUMENTS] =
            navBackStackEntry.savedStateHandle.get<Map<String, Any>>(EXTRA_BACK_NAVIGATION_ARGUMENTS)
    }

fun <V : Any> SavedStateHandle.getBackNavArgs() = get<Map<String, V>>(EXTRA_BACK_NAVIGATION_ARGUMENTS) ?: mapOf()

@Suppress("UNCHECKED_CAST")
fun <V : Any> SavedStateHandle.getBackNavArg(key: String): V? = getBackNavArgs<Any>()[key] as? V
