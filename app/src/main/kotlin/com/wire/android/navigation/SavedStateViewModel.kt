package com.wire.android.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap

/*
 * ViewModel extension which already has SavedStateHandle field.
 * It is recommended to use it for NavigationItem together with the hiltViewModel(backNavArgs: ImmutableMap<String, Any>) method which
 * requires a map of back navigation arguments which are passed to the NavBackStackEntry's SavedStateHandle when using popWithArguments.
 */
open class SavedStateViewModel(open val savedStateHandle: SavedStateHandle) : ViewModel()

@Composable
inline fun <reified T : SavedStateViewModel> hiltSavedStateViewModel(backNavArgs: ImmutableMap<String, Any>) =
    hiltViewModel<T>().apply {
        savedStateHandle[EXTRA_BACK_NAVIGATION_ARGUMENTS] = backNavArgs.toMap()
    }

fun <V : Any> SavedStateHandle.getBackNavArgs(): ImmutableMap<String, V> =
    get<Map<String, V>>(EXTRA_BACK_NAVIGATION_ARGUMENTS)?.toImmutableMap() ?: persistentMapOf()

@Suppress("UNCHECKED_CAST")
fun <V : Any> SavedStateHandle.getBackNavArg(key: String): V? = getBackNavArgs<Any>()[key] as? V
