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

package com.wire.android.navigation.runtime

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wire.android.appLogger
import com.wire.android.di.metro.AppSessionViewModelGraph
import com.wire.android.di.metro.WireApplicationGraph
import com.wire.android.di.metro.createSessionViewModelGraph
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutCallback
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * The single creation boundary for the Activity-retained session graph registry.
 *
 * Feature ViewModels are created through the Metro gateway. This infrastructure ViewModel owns
 * Metro session graphs themselves, so it must be created by the framework before a session graph
 * exists and is deliberately isolated behind this runtime-specific gateway.
 */
@Composable
internal fun wireSessionGraphStoreViewModel(
    appGraph: WireApplicationGraph,
    owner: ViewModelStoreOwner,
): SessionGraphStoreViewModel = viewModel(
    viewModelStoreOwner = owner,
    factory = viewModelFactory {
        initializer { SessionGraphStoreViewModel(appGraph) }
    },
)

internal class SessionGraphStoreViewModel(
    private val createSessionGraph: (UserId) -> AppSessionViewModelGraph,
    private val registerLogoutCallback: ((LogoutCallback) -> Unit)? = null,
    private val unregisterLogoutCallback: ((LogoutCallback) -> Unit)? = null,
) : ViewModel() {

    /*
     * Host lifecycle commands are ordered on the main thread: invalidate, remove all consumers,
     * dispose, then make a newly authenticated generation available. Synchronization here closes
     * graph-creation/logout races; it does not make removal a revocable lease for callers that
     * already obtained a graph.
     */

    internal enum class Lifecycle {
        ACTIVE,
        INVALIDATING,
        REMOVED,
    }

    private sealed interface Entry {
        data class Creating(val result: CompletableFuture<AppSessionViewModelGraph>) : Entry
        data class Active(val graph: AppSessionViewModelGraph) : Entry
        data class Invalidating(val graph: AppSessionViewModelGraph?) : Entry
        data class Releasing(val result: CompletableFuture<AppSessionViewModelGraph>) : Entry
        data object Removed : Entry
    }

    constructor(appGraph: WireApplicationGraph) : this(
        createSessionGraph = appGraph::createSessionViewModelGraph,
        registerLogoutCallback = appGraph.coreLogic.getGlobalScope().logoutCallbackManager::register,
        unregisterLogoutCallback = appGraph.coreLogic.getGlobalScope().logoutCallbackManager::unregister,
    )

    private val entries = mutableMapOf<UserId, Entry>()
    private var cleared = false
    private val logoutCallback = object : LogoutCallback {
        override suspend fun invoke(userId: UserId, reason: LogoutReason) {
            // The callback can race the Activity's navigation effects. It may block stale graph
            // resolution immediately, but final disposal belongs to the ordered host coordinator:
            // entry effects -> ViewModelStore -> Metro graph.
            markInvalidating(userId)
        }
    }

    init {
        registerLogoutCallback?.invoke(logoutCallback)
    }

    @Suppress("CyclomaticComplexMethod", "ReturnCount", "ThrowsCount", "TooGenericExceptionCaught")
    fun graphFor(userId: UserId): AppSessionViewModelGraph {
        var createsGraph = false
        val result = synchronized(this) {
            if (cleared) throw unavailable(Lifecycle.REMOVED)
            when (val entry = entries[userId]) {
                is Entry.Active -> return entry.graph
                is Entry.Invalidating,
                is Entry.Releasing,
                Entry.Removed -> throw unavailable(lifecycleFor(entry))

                is Entry.Creating -> entry.result
                null -> CompletableFuture<AppSessionViewModelGraph>().also {
                    createsGraph = true
                    entries[userId] = Entry.Creating(it)
                }
            }
        }

        if (!createsGraph) return result.awaitGraph()

        val graph = try {
            appLogger.i("WireActivity creating lifecycle-retained session graph for $REDACTED_SESSION_LABEL")
            createSessionGraph(userId)
        } catch (failure: Throwable) {
            synchronized(this) {
                if ((entries[userId] as? Entry.Creating)?.result === result) {
                    entries.remove(userId)
                }
            }
            result.completeExceptionally(failure)
            throw failure
        }

        val unavailableLifecycle = synchronized(this) {
            when (val entry = entries[userId]) {
                is Entry.Creating -> if (entry.result === result) {
                    entries[userId] = Entry.Active(graph)
                    result.complete(graph)
                    null
                } else {
                    Lifecycle.REMOVED
                }

                is Entry.Releasing -> {
                    if (entry.result === result) entries.remove(userId)
                    Lifecycle.REMOVED
                }

                is Entry.Invalidating -> Lifecycle.INVALIDATING
                Entry.Removed -> Lifecycle.REMOVED
                is Entry.Active,
                null -> Lifecycle.REMOVED
            }
        }
        if (unavailableLifecycle == null) return graph

        val failure = unavailable(unavailableLifecycle)
        result.completeExceptionally(failure)
        dispose(graph, "discarded after lifecycle changed")
        throw failure
    }

    /** Marks a session unavailable before its remaining navigation entries are torn down. */
    internal fun markInvalidating(userId: UserId) {
        synchronized(this) {
            if (cleared) return
            when (val entry = entries[userId]) {
                is Entry.Active -> entries[userId] = Entry.Invalidating(entry.graph)
                is Entry.Creating -> {
                    entries[userId] = Entry.Invalidating(graph = null)
                    entry.result.completeExceptionally(unavailable(Lifecycle.INVALIDATING))
                }

                null -> entries[userId] = Entry.Invalidating(graph = null)
                is Entry.Releasing -> entries[userId] = Entry.Invalidating(graph = null)
                is Entry.Invalidating,
                Entry.Removed -> Unit
            }
        }
        appLogger.i("WireActivity marked session graph invalidating for $REDACTED_SESSION_LABEL")
    }

    /** Completes removal and keeps a tombstone that rejects stale route composition. */
    internal fun markRemoved(userId: UserId) {
        val graphToDispose = synchronized(this) {
            if (cleared) return
            when (val entry = entries[userId]) {
                is Entry.Active -> entry.graph
                is Entry.Invalidating -> entry.graph
                is Entry.Creating -> {
                    entry.result.completeExceptionally(unavailable(Lifecycle.REMOVED))
                    null
                }

                is Entry.Releasing,
                Entry.Removed,
                null -> null
            }
                .also { entries[userId] = Entry.Removed }
        }
        graphToDispose?.let { dispose(it, "marked removed") }
    }

    /** Starts a new graph generation after the session has been independently confirmed valid. */
    internal fun markAvailable(userId: UserId) {
        val reopened = synchronized(this) {
            if (cleared) return
            when (entries[userId]) {
                Entry.Removed -> {
                    entries.remove(userId)
                    true
                }

                is Entry.Invalidating -> error(
                    "Session graph must be REMOVED before it can become available again"
                )

                is Entry.Active,
                is Entry.Creating,
                is Entry.Releasing,
                null -> false
            }
        }
        if (reopened) {
            appLogger.i("WireActivity reopened session graph lifecycle for $REDACTED_SESSION_LABEL")
        }
    }

    /**
     * Releases an Activity-local graph without invalidating the underlying Kalium session.
     *
     * Call activities use this when a reused singleTop/singleInstance Activity receives an intent
     * for another valid account. Unlike [markRemoved], this deliberately leaves no tombstone.
    */
    internal fun release(userId: UserId) {
        val graphToDispose = synchronized(this) {
            if (cleared) return
            when (val entry = entries[userId]) {
                is Entry.Active -> {
                    entries.remove(userId)
                    entry.graph
                }

                is Entry.Creating -> {
                    entries[userId] = Entry.Releasing(entry.result)
                    entry.result.completeExceptionally(unavailable(Lifecycle.REMOVED))
                    null
                }

                is Entry.Invalidating -> {
                    entries[userId] = Entry.Removed
                    entry.graph
                }

                is Entry.Releasing,
                Entry.Removed,
                null -> null
            }
        }
        graphToDispose?.let { dispose(it, "released") }
    }

    internal fun lifecycle(userId: UserId): Lifecycle? = synchronized(this) {
        entries[userId]?.let(::lifecycleFor)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun dispose(graph: AppSessionViewModelGraph, reason: String) {
        appLogger.i("WireActivity $reason lifecycle-retained session graph for $REDACTED_SESSION_LABEL")
        try {
            graph.wireSessionImageLoader.shutdown()
        } catch (failure: Exception) {
            appLogger.e("WireActivity failed to dispose a lifecycle-retained session graph", failure)
        }
    }

    private fun lifecycleFor(entry: Entry): Lifecycle = when (entry) {
        is Entry.Creating,
        is Entry.Active -> Lifecycle.ACTIVE
        is Entry.Invalidating -> Lifecycle.INVALIDATING
        is Entry.Releasing,
        Entry.Removed -> Lifecycle.REMOVED
    }

    override fun onCleared() {
        unregisterLogoutCallback?.invoke(logoutCallback)
        val graphs = synchronized(this) {
            cleared = true
            val retainedGraphs = entries.mapNotNull { (_, entry) ->
                when (entry) {
                    is Entry.Active -> entry.graph
                    is Entry.Invalidating -> entry.graph
                    is Entry.Creating -> {
                        entry.result.completeExceptionally(unavailable(Lifecycle.REMOVED))
                        null
                    }

                    is Entry.Releasing -> {
                        entry.result.completeExceptionally(unavailable(Lifecycle.REMOVED))
                        null
                    }

                    Entry.Removed -> null
                }
            }
            entries.clear()
            retainedGraphs
        }
        graphs.forEach { graph -> dispose(graph, "cleared") }
    }

    private fun unavailable(lifecycle: Lifecycle) =
        SessionGraphUnavailableException(REDACTED_SESSION_LABEL, lifecycle)

    private companion object {
        const val REDACTED_SESSION_LABEL = "session:<redacted>"
    }
}

internal class SessionGraphUnavailableException(
    sessionLabel: String,
    val lifecycle: SessionGraphStoreViewModel.Lifecycle,
) : IllegalStateException("Session graph for $sessionLabel is $lifecycle")

private fun CompletableFuture<AppSessionViewModelGraph>.awaitGraph(): AppSessionViewModelGraph =
    try {
        join()
    } catch (failure: CompletionException) {
        throw (failure.cause ?: failure)
    }
