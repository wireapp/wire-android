/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.runtime

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.di.metro.WireViewModelDiagnosticSink
import com.wire.navigation.WireBackStackChange
import com.wire.navigation.WireRoute
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Opt-in diagnostics shared by auth routing, Navigation 3, Metro graph resolution and ViewModels.
 *
 * Enable for a local build with `-Pwire.navigation.diagnostics=true`. Route arguments and account
 * identifiers are deliberately excluded from the output.
 */
internal object WireNavigationDiagnostics : WireViewModelDiagnosticSink {
    private const val PREFIX = "[WireNav3]"
    private val sequence = AtomicLong()
    private val ownerKeys = WeakHashMap<ViewModelStoreOwner, String>()
    private val stores = WeakHashMap<ViewModelStore, StoreDiagnostics>()

    val enabled: Boolean
        get() = BuildConfig.NAVIGATION_DIAGNOSTICS_ENABLED

    fun nextTransitionId(): Long = sequence.incrementAndGet()

    fun auth(transitionId: Long, event: String, outcome: String) =
        log("AUTH", "id=$transitionId event=$event outcome=$outcome")

    fun navigation(
        previous: List<WireRoute>,
        current: List<WireRoute>,
        change: WireBackStackChange,
    ) = log(
        "NAV",
        "change=$change from=${previous.routeIds()} to=${current.routeIds()}",
    )

    fun metro(route: WireRoute, scope: String, outcome: String) =
        log("METRO", "route=${route.routeId} scope=$scope outcome=$outcome")

    fun viewModel(type: String, factory: String) =
        log("VM", "type=$type factory=$factory")

    @Synchronized
    override fun ownerAvailable(owner: ViewModelStoreOwner, ownerKey: String) {
        val store = owner.viewModelStore
        val previousOwnerKey = ownerKeys[owner]
        check(previousOwnerKey == null || previousOwnerKey == ownerKey) {
            "One ViewModelStoreOwner cannot represent both " +
                    "${previousOwnerKey?.redactedOwnerKey()} and ${ownerKey.redactedOwnerKey()}"
        }
        val conflictingStore = ownerKeys
            .asSequence()
            .filter { (_, activeOwnerKey) -> activeOwnerKey == ownerKey }
            .map { (activeOwner) -> activeOwner.viewModelStore }
            .firstOrNull { activeStore -> activeStore !== store }
        check(conflictingStore == null) {
            "Active owner ${ownerKey.redactedOwnerKey()} maps to multiple ViewModelStores"
        }
        ownerKeys[owner] = ownerKey
        val storeDiagnostics = stores.getOrPut(store) { StoreDiagnostics() }
        if (previousOwnerKey == null) {
            storeDiagnostics.references++
        }
        log(
            "OWNER",
            "owner=${ownerKey.redactedOwnerKey()} store=${store.storeId()} " +
                    "outcome=${if (storeDiagnostics.references == 1) "acquire" else "reuse"}",
        )
    }

    @Synchronized
    override fun ownerReleased(owner: ViewModelStoreOwner, ownerKey: String) {
        val store = owner.viewModelStore
        if (ownerKeys.remove(owner) != null) {
            stores[store]?.let { diagnostics ->
                diagnostics.references = (diagnostics.references - 1).coerceAtLeast(0)
            }
        }
        log(
            "OWNER",
            "owner=${ownerKey.redactedOwnerKey()} store=${store.storeId()} outcome=release",
        )
    }

    @Synchronized
    override fun ownerCleared(ownerKey: String) {
        val matchingOwners = ownerKeys
            .filterValues { it == ownerKey }
            .keys
            .toList()
        val matchingStores = matchingOwners.map(ViewModelStoreOwner::viewModelStore).toSet()
        matchingOwners.forEach(ownerKeys::remove)
        matchingStores.forEach(stores::remove)
        log("OWNER", "owner=${ownerKey.redactedOwnerKey()} outcome=clear")
    }

    @Synchronized
    override fun viewModelRequested(owner: ViewModelStoreOwner, viewModelKey: String) {
        val store = owner.viewModelStore
        val diagnostics = stores.getOrPut(store) { StoreDiagnostics() }
        val firstRequest = diagnostics.viewModelKeys.add(viewModelKey)
        log(
            "VM",
            "owner=${ownerKeys[owner]?.redactedOwnerKey() ?: "unregistered"} " +
                    "store=${store.storeId()} key=${viewModelKey.redactedViewModelKey()} " +
                    "outcome=${if (firstRequest) "create-request" else "reuse-request"}",
        )
    }

    private fun log(channel: String, message: String) {
        if (enabled) appLogger.i("$PREFIX[$channel] $message")
    }

    private fun List<WireRoute>.routeIds(): String =
        joinToString(prefix = "[", postfix = "]") { it.routeId }

    private fun String.redactedOwnerKey(): String =
        if (startsWith("session:")) "session:<redacted>" else this

    private fun ViewModelStore.storeId(): String =
        Integer.toHexString(System.identityHashCode(this))

    private class StoreDiagnostics(
        var references: Int = 0,
        val viewModelKeys: MutableSet<String> = mutableSetOf(),
    )
}

internal fun String.redactedViewModelKey(): String = substringBefore(':')
