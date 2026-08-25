/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.navigation.navigation3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import com.wire.navigation.WireSessionId
import com.wire.navigation.WireViewModelOwner
import com.wire.navigation.stableKey

val LocalWireViewModelOwner = staticCompositionLocalOf<WireViewModelOwner.Entry?> { null }

val LocalWireSharedViewModelStoreOwners =
    staticCompositionLocalOf<Map<WireViewModelOwner, ViewModelStoreOwner>> { emptyMap() }

/**
 * Resolves one typed owner made available by the current route.
 */
@Composable
fun wireViewModelStoreOwner(owner: WireViewModelOwner): ViewModelStoreOwner =
    when (owner) {
        is WireViewModelOwner.Entry -> {
            val currentOwner = checkNotNull(LocalWireViewModelOwner.current) {
                "No Wire entry ViewModel owner is available"
            }
            require(currentOwner == owner) {
                "Entry owner ${owner.stableKey()} does not belong to the current route"
            }
            checkNotNull(LocalViewModelStoreOwner.current)
        }

        else -> checkNotNull(LocalWireSharedViewModelStoreOwners.current[owner]) {
            "Shared owner ${owner.stableKey()} is not available to the current route"
        }
    }

/**
 * Shared-owner decorator based on Navigation 3's shared ViewModel recipe.
 *
 * The standard entry decorator remains the default [LocalViewModelStoreOwner]. This decorator
 * retains additional flow, session and application stores under stable typed owner keys.
 */
class WireViewModelStoreNavEntryDecorator private constructor(
    private val registry: WireViewModelStoreRegistry,
    private val onOwnerAvailable: (WireViewModelOwner, ViewModelStoreOwner) -> Unit,
    private val onOwnerReleased: (WireViewModelOwner, ViewModelStoreOwner) -> Unit,
) : NavEntryDecorator<NavKey>(
    onPop = registry::onEntryPopped,
    decorate = { entry ->
        val entryOwnerIdentity = checkNotNull(entry.metadata[EntryOwnerKey]) {
            "Wire entry ${entry.contentKey} has no typed ViewModel owner metadata; register it with wireEntry"
        }
        val sharedOwnerIdentities = entry.metadata[SharedOwnersKey].orEmpty()
        DisposableEffect(entry.contentKey, sharedOwnerIdentities) {
            registry.registerEntry(entry.contentKey, sharedOwnerIdentities)
            onDispose { }
        }

        val entryOwner = checkNotNull(LocalViewModelStoreOwner.current) {
            "The standard Navigation 3 entry ViewModelStore decorator must run first"
        }
        DisposableEffect(entryOwnerIdentity, entryOwner) {
            onOwnerAvailable(entryOwnerIdentity, entryOwner)
            onDispose {
                onOwnerReleased(entryOwnerIdentity, entryOwner)
            }
        }
        val sharedOwners = linkedMapOf<WireViewModelOwner, ViewModelStoreOwner>()
        sharedOwnerIdentities
            .sortedBy(WireViewModelOwner::stableKey)
            .forEach { ownerIdentity ->
                key(ownerIdentity.stableKey()) {
                    val owner = rememberViewModelStoreOwner(
                        key = ownerIdentity.stableKey(),
                        provider = registry.storeProvider,
                    )
                    DisposableEffect(ownerIdentity, owner) {
                        onOwnerAvailable(ownerIdentity, owner)
                        onDispose {
                            onOwnerReleased(ownerIdentity, owner)
                        }
                    }
                    sharedOwners[ownerIdentity] = owner
                }
            }
        CompositionLocalProvider(
            LocalWireViewModelOwner provides entryOwnerIdentity,
            LocalWireSharedViewModelStoreOwners provides sharedOwners,
        ) {
            entry.Content()
        }
    },
) {
    /**
     * Explicitly releases a session or application owner after its navigation entries stop using
     * it. Flow owners are released automatically when their last entry is popped.
     */
    fun clear(owner: WireViewModelOwner) {
        require(owner !is WireViewModelOwner.Entry) {
            "Entry owners are cleared by the standard Navigation 3 decorator"
        }
        registry.clear(owner)
    }

    constructor(
        storeProvider: ViewModelStoreProvider,
        onOwnerAvailable: (WireViewModelOwner, ViewModelStoreOwner) -> Unit = { _, _ -> },
        onOwnerReleased: (WireViewModelOwner, ViewModelStoreOwner) -> Unit = { _, _ -> },
        onOwnerCleared: (String) -> Unit = {},
    ) : this(
        registry = WireViewModelStoreRegistry(storeProvider, onOwnerCleared),
        onOwnerAvailable = onOwnerAvailable,
        onOwnerReleased = onOwnerReleased,
    )

    companion object {
        private object EntryOwnerKey : NavMetadataKey<WireViewModelOwner.Entry>
        private object SharedOwnersKey : NavMetadataKey<Set<WireViewModelOwner>>

        fun owners(
            entryOwner: WireViewModelOwner.Entry,
            sharedOwners: Set<WireViewModelOwner>,
        ) = metadata {
            put(EntryOwnerKey, entryOwner)
            put(SharedOwnersKey, sharedOwners)
        }

        fun flow(flowId: String?) = metadata {
            if (flowId != null) {
                put(SharedOwnersKey, setOf(WireViewModelOwner.Flow(flowId)))
            }
        }
    }
}

internal class WireViewModelStoreRegistry(
    internal val storeProvider: ViewModelStoreProvider,
    private val onOwnerCleared: (String) -> Unit = {},
) {
    private val ownershipTracker = WireViewModelOwnershipTracker(::clearOwner)

    fun registerEntry(contentKey: Any, owners: Set<WireViewModelOwner>) {
        ownershipTracker.registerEntry(contentKey, owners)
    }

    fun viewModelStoreFor(owner: WireViewModelOwner) =
        storeProvider.getOrCreate(owner.stableKey())

    fun onEntryPopped(contentKey: Any) {
        ownershipTracker.onEntryPopped(contentKey)
    }

    fun clearSession(sessionId: WireSessionId) {
        clear(WireViewModelOwner.Session(sessionId))
    }

    fun clearApplication() {
        clear(WireViewModelOwner.Application)
    }

    fun clear(owner: WireViewModelOwner) {
        clearOwner(owner.stableKey())
    }

    private fun clearOwner(ownerKey: String) {
        clearWireViewModelStoreOwner(storeProvider, ownerKey) {
            onOwnerCleared(ownerKey)
        }
    }
}

private class OwnerClearObserver(
    private val onOwnerCleared: () -> Unit,
) : ViewModel() {
    override fun onCleared() {
        onOwnerCleared()
    }
}

/**
 * Marks a shared owner for removal and reports only the actual store clear. Lifecycle defers that
 * clear while an exit animation or another composition still holds a provider token.
 */
fun clearWireViewModelStoreOwner(
    provider: ViewModelStoreProvider,
    key: String,
    onCleared: () -> Unit = {},
) {
    val store = provider.getOrCreate(key)
    ViewModelProvider.create(
        store = store,
        factory = viewModelFactory {
            initializer { OwnerClearObserver(onCleared) }
        },
    )[OwnerClearObserver::class]
    provider.clearKey(key)
}

internal class WireViewModelOwnershipTracker(
    private val onOwnerReleased: (String) -> Unit,
) {
    private val entryOwners = mutableMapOf<Any, Set<WireViewModelOwner>>()

    fun registerEntry(contentKey: Any, owners: Set<WireViewModelOwner>) {
        val previous = entryOwners.put(contentKey, owners).orEmpty()
        (previous - owners).forEach(::releaseIfUnreferenced)
    }

    fun onEntryPopped(contentKey: Any) {
        entryOwners.remove(contentKey).orEmpty().forEach(::releaseIfUnreferenced)
    }

    private fun releaseIfUnreferenced(owner: WireViewModelOwner) {
        if (owner !is WireViewModelOwner.Entry && owner !is WireViewModelOwner.Flow) return
        if (entryOwners.values.none { owner in it }) {
            onOwnerReleased(owner.stableKey())
        }
    }
}

@Composable
fun rememberWireViewModelStoreProvider(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No parent ViewModelStoreOwner was provided for Wire ViewModels"
    },
): ViewModelStoreProvider = rememberViewModelStoreProvider(
    key = "wire-navigation-view-model-owners",
    parent = viewModelStoreOwner,
)

/** Typed Wire name for Lifecycle's shared-owner composable API. */
@Composable
fun rememberWireSharedViewModelStoreOwner(
    key: String,
    provider: ViewModelStoreProvider,
): ViewModelStoreOwner = rememberViewModelStoreOwner(
    key = key,
    provider = provider,
)

@Composable
fun rememberWireViewModelStoreNavEntryDecorator(
    storeProvider: ViewModelStoreProvider,
    onOwnerAvailable: (WireViewModelOwner, ViewModelStoreOwner) -> Unit = { _, _ -> },
    onOwnerReleased: (WireViewModelOwner, ViewModelStoreOwner) -> Unit = { _, _ -> },
    onOwnerCleared: (String) -> Unit = {},
): WireViewModelStoreNavEntryDecorator =
    androidx.compose.runtime.remember(
        storeProvider,
        onOwnerAvailable,
        onOwnerReleased,
        onOwnerCleared,
    ) {
        WireViewModelStoreNavEntryDecorator(
            storeProvider = storeProvider,
            onOwnerAvailable = onOwnerAvailable,
            onOwnerReleased = onOwnerReleased,
            onOwnerCleared = onOwnerCleared,
        )
    }

@Composable
fun rememberWireViewModelStoreNavEntryDecorator(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No parent ViewModelStoreOwner was provided for Wire ViewModels"
    },
): WireViewModelStoreNavEntryDecorator =
    rememberWireViewModelStoreNavEntryDecorator(
        rememberWireViewModelStoreProvider(viewModelStoreOwner)
    )
