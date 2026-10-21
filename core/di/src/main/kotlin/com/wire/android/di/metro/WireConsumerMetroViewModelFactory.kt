/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.di.metro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Defers opening a consumer graph until ViewModelProvider actually needs a new ViewModel.
 *
 * Metro's create method is final. Its assisted lookup is therefore used as a dispatch hook for
 * both direct and assisted creation; the real binding maps are resolved by [createInGraph].
 * No graph, consumer, or manually assisted factory is retained by this session-level dispatcher.
 */
abstract class WireConsumerMetroViewModelFactory : MetroViewModelFactory() {
    final override val assistedFactoryProviders:
        Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory> =
        object : Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory> by emptyMap() {
            override fun get(key: KClass<out ViewModel>): () -> ViewModelAssistedFactory = {
                object : ViewModelAssistedFactory {
                    override fun create(extras: CreationExtras): ViewModel = createInGraph { factory ->
                        factory.create(key, extras)
                    }
                }
            }
        }

    protected abstract fun <VM : ViewModel> createInGraph(create: (MetroViewModelFactory) -> VM): VM

    fun <VM : ViewModel, Factory : ManualViewModelAssistedFactory> createManuallyAssistedViewModel(
        modelClass: KClass<VM>,
        factoryClass: KClass<Factory>,
        extras: CreationExtras,
        create: Factory.(CreationExtras) -> ViewModel,
    ): VM = createInGraph { factory ->
        // Resolve the factory and run the whole callback inside the consumer's failure boundary.
        modelClass.cast(factory.createManuallyAssistedFactory(factoryClass)().create(extras))
    }
}

/** Shared by navigation-owned and Resaca-owned ViewModels; lookup remains with the caller's owner. */
fun <VM : ViewModel, Factory : ManualViewModelAssistedFactory> MetroViewModelFactory.wireManualViewModelFactory(
    factoryClass: KClass<Factory>,
    create: Factory.(CreationExtras) -> VM,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
        if (this@wireManualViewModelFactory is WireConsumerMetroViewModelFactory) {
            createManuallyAssistedViewModel(modelClass, factoryClass, extras, create)
        } else {
            modelClass.cast(createManuallyAssistedFactory(factoryClass)().create(extras))
        }
}
