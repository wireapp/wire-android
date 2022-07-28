package com.wire.android.di

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import cafe.adriel.voyager.androidx.AndroidScreenLifecycleOwner
import cafe.adriel.voyager.core.lifecycle.ScreenLifecycleProvider
import cafe.adriel.voyager.core.screen.Screen
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder
import dagger.hilt.android.internal.lifecycle.HiltViewModelFactory
import dagger.hilt.android.internal.lifecycle.HiltViewModelMap
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

const val KEY_PARAM = "key_param"

interface AssistedViewModel<P : Any?> {
    val savedStateHandle: SavedStateHandle
    val param: P get() = savedStateHandle.get(KEY_PARAM)!!
}

@Composable
inline fun <reified P : Any, reified T> Screen.getAssistedViewModel(param: P?): T where T : ViewModel, T : AssistedViewModel<P> {
    val context = LocalContext.current
    return remember(key1 = T::class) {
        val activity = context.activity
        val lifecycleOwner = (this as? ScreenLifecycleProvider)?.getLifecycleOwner() as? AndroidScreenLifecycleOwner
        val viewModelStore = lifecycleOwner?.viewModelStore ?: activity.viewModelStore
        val bundleArgs = bundleOf(KEY_PARAM to param)
        val factory = getAssistedFactory(activity, lifecycleOwner ?: activity, bundleArgs)
        val provider = ViewModelProvider(viewModelStore, factory)
        provider[T::class.java]
    }
}

val Context.activity: ComponentActivity
    get() {
        when (this) {
            is ComponentActivity -> return this
            else -> {
                var context = this
                while (context is ContextWrapper) {
                    context = context.baseContext
                    if (context is ComponentActivity) return context
                }
                error("Context $this is not a androidx.activity.ComponentActivity")
            }
        }
    }

fun getAssistedFactory(activity: ComponentActivity, owner: SavedStateRegistryOwner, args: Bundle?): ViewModelProvider.Factory =
    EntryPoints.get(activity, AssistedViewModelFactoryEntryPoint::class.java)
        .assistedViewModelFactory()
        .factory(owner, args)

internal class AssistedViewModelFactory @Inject internal constructor(
    private val application: Application,
    @HiltViewModelMap.KeySet private val keySet: Set<String>,
    private val viewModelComponentBuilder: ViewModelComponentBuilder
) {
    fun factory(
        owner: SavedStateRegistryOwner,
        args: Bundle?
    ): ViewModelProvider.Factory {
        val delegate = SavedStateViewModelFactory(application, owner, args)
        return HiltViewModelFactory(owner, args, keySet, delegate, viewModelComponentBuilder)
    }
}

@EntryPoint
@InstallIn(ActivityComponent::class)
internal interface AssistedViewModelFactoryEntryPoint {
    fun assistedViewModelFactory(): AssistedViewModelFactory
}

@Parcelize
private class NavQualifiedId private constructor(private val qualifiedIdString: String) : Parcelable {
    constructor(qualifiedId: QualifiedID) : this(qualifiedId.toString())
    private val qualifiedId: QualifiedID get() = qualifiedIdString.parseIntoQualifiedID()
    @IgnoredOnParcel
    val value: String get() = qualifiedId.value
    @IgnoredOnParcel
    val domain: String get() = qualifiedId.domain
}
