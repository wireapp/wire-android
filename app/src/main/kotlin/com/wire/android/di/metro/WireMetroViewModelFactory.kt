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
package com.wire.android.di.metro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.wire.android.navigation.runtime.WireNavigationDiagnostics
import com.wire.kalium.logic.di.KaliumConsumer
import com.wire.kalium.logic.feature.UserSessionScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Job
import kotlin.reflect.KClass
import kotlin.reflect.cast

abstract class WireMetroViewModelFactory(
    viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    manualAssistedFactoryProviders:
    Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : MetroViewModelFactory() {
    override val viewModelProviders = viewModelProviders.withDiagnostics("direct")
    override val assistedFactoryProviders = assistedFactoryProviders.withDiagnostics("assisted")
    override val manualAssistedFactoryProviders =
        manualAssistedFactoryProviders.withDiagnostics("manual-assisted")
}

/** Application graph factory. It must only see application-owned ViewModel bindings. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<MetroViewModelFactory>())
class WireApplicationMetroViewModelFactory(
    viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    manualAssistedFactoryProviders:
    Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : WireMetroViewModelFactory(
    viewModelProviders,
    assistedFactoryProviders,
    manualAssistedFactoryProviders,
)

/** Session dispatcher. Only the factory is retained; every new ViewModel gets a child graph. */
@Inject
@SingleIn(MetroSessionScope::class)
@ContributesBinding(MetroSessionScope::class, binding = binding<MetroViewModelFactory>())
class WireSessionMetroViewModelFactory(
    private val graphFactory: AppViewModelGraph.Factory,
    private val session: UserSessionScope,
) : WireConsumerMetroViewModelFactory() {
    // Parent providers can still look up by account. The captured consumer guards the entire
    // assembly, rejecting publication if those providers cross into a replacement generation.
    override fun <VM : ViewModel> createInGraph(create: (MetroViewModelFactory) -> VM): VM =
        createConsumerViewModel(
            createConsumer = session::createConsumer,
        ) { consumer ->
            create(graphFactory.createViewModelGraph(consumer).consumerViewModelFactory)
        }
}

/** Constructed in AppViewModelGraph so all provider maps and manual factories belong to that child. */
@Inject
class WireViewModelGraphFactory(
    viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    manualAssistedFactoryProviders:
    Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : WireMetroViewModelFactory(
    viewModelProviders,
    assistedFactoryProviders,
    manualAssistedFactoryProviders,
)

/**
 * The ViewModel owns its consumer. Invalidation drains guarded SDK work, while the published
 * ViewModel's own job can finish terminal account work and its follow-up. Only an unpublished,
 * rejected ViewModel transfers its job to consumer drain so resources outlive its cleanup.
 */
@Suppress("TooGenericExceptionCaught") // Cleanup must preserve any original construction failure, including Errors.
internal fun <VM : ViewModel> createConsumerViewModel(
    createConsumer: () -> KaliumConsumer,
    create: (KaliumConsumer) -> VM,
): VM {
    val consumer = createConsumer()
    val closeable = CloseOnce(consumer)
    try {
        return consumer.assemble(
            create = { create(consumer).also { it.addCloseable(closeable) } },
            onDiscard = ::clearUnpublishedViewModel,
            jobOnDiscard = { it.viewModelScope.coroutineContext[Job] },
        )
    } catch (failure: Throwable) {
        try {
            closeable.close()
        } catch (cleanupFailure: Throwable) {
            if (cleanupFailure !== failure) failure.addSuppressed(cleanupFailure)
        }
        throw failure
    }
}

/** Clear through public lifecycle APIs, including onCleared and resources beyond viewModelScope. */
private fun clearUnpublishedViewModel(viewModel: ViewModel) {
    val store = ViewModelStore()
    val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = modelClass.cast(viewModel)
    }
    ViewModelProvider.create(store, factory)["unpublished", viewModel::class]
    store.clear()
}

private class CloseOnce(private val consumer: AutoCloseable) : AutoCloseable {
    private val closed = AtomicBoolean()

    override fun close() {
        if (closed.compareAndSet(false, true)) consumer.close()
    }
}

private fun <K : Any, V : Any> Map<KClass<out K>, () -> V>.withDiagnostics(
    factory: String,
): Map<KClass<out K>, () -> V> =
    mapValues { (type, provider) ->
        {
            WireNavigationDiagnostics.viewModel(
                type = type.qualifiedName ?: type.simpleName ?: "anonymous",
                factory = factory,
            )
            provider()
        }
    }
