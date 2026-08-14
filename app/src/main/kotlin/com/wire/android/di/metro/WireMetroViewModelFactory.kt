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
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import com.wire.android.navigation.runtime.WireNavigationDiagnostics
import kotlin.reflect.KClass

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

/** Session graph factory. It must be built from the selected session graph's binding maps. */
@Inject
@SingleIn(MetroSessionScope::class)
@ContributesBinding(MetroSessionScope::class, binding = binding<MetroViewModelFactory>())
class WireSessionMetroViewModelFactory(
    viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    manualAssistedFactoryProviders:
    Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : WireMetroViewModelFactory(
    viewModelProviders,
    assistedFactoryProviders,
    manualAssistedFactoryProviders,
)

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
