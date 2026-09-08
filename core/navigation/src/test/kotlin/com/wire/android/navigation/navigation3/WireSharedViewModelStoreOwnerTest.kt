/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.navigation3

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState
import kotlin.reflect.KClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class WireSharedViewModelStoreOwnerTest {

    @Test
    fun givenStableStoreAndKey_whenOwnerWrapperChanges_thenExistingViewModelIsReused() {
        val provider = ViewModelStoreProvider(parentStore = null)
        val markerKey = CreationExtras.Key<String>()
        val firstRegistryOwner = TestSavedStateRegistryOwner()
        val firstOwner = provider.owner("flow:login", firstRegistryOwner, TestFactory(markerKey))
        firstRegistryOwner.moveToCreated()
        val first = ViewModelProvider.create(firstOwner)[TestSavedStateViewModel::class]

        val secondRegistryOwner = TestSavedStateRegistryOwner()
        val secondOwner = provider.owner("flow:login", secondRegistryOwner, TestFactory(markerKey))
        secondRegistryOwner.moveToCreated()
        val second = ViewModelProvider.create(secondOwner)[TestSavedStateViewModel::class]

        assertSame(first, second)
    }

    @Test
    fun givenParentFactoryAndExtras_whenFlowViewModelIsCreated_thenBothReachTheFactory() {
        val savedStateOwner = TestSavedStateRegistryOwner()
        val markerKey = CreationExtras.Key<String>()
        val factory = TestFactory(markerKey)
        val owner = ViewModelStoreProvider(parentStore = null).getOrCreateOwner(
            key = "flow:login",
            savedStateRegistryOwner = savedStateOwner,
            defaultFactory = factory,
            defaultCreationExtras = MutableCreationExtras().apply { this[markerKey] = METRO_MARKER },
        )
        savedStateOwner.moveToCreated()

        val viewModel = ViewModelProvider.create(owner)[TestSavedStateViewModel::class]

        assertEquals(METRO_MARKER, factory.receivedMarker)
        assertEquals(0, viewModel.savedStateHandle[COUNT_KEY])
    }

    @Test
    fun givenFlowViewModelState_whenOwnerIsRecreated_thenSavedStateHandleIsRestored() {
        val markerKey = CreationExtras.Key<String>()
        val firstRegistryOwner = TestSavedStateRegistryOwner()
        val firstFlowOwner = ViewModelStoreProvider(parentStore = null)
            .owner("flow:login", firstRegistryOwner, TestFactory(markerKey))
        firstRegistryOwner.moveToCreated()
        ViewModelProvider.create(firstFlowOwner)[TestSavedStateViewModel::class]
            .savedStateHandle[COUNT_KEY] = RESTORED_COUNT
        val savedState = firstRegistryOwner.save()
        assertTrue(savedState.read { !isEmpty() })

        val restoredRegistryOwner = TestSavedStateRegistryOwner(savedState)
        val restoredFlowOwner = ViewModelStoreProvider(parentStore = null)
            .owner("flow:login", restoredRegistryOwner, TestFactory(markerKey))
        restoredRegistryOwner.moveToCreated()

        val restored = ViewModelProvider.create(restoredFlowOwner)[TestSavedStateViewModel::class]

        assertEquals(RESTORED_COUNT, restored.savedStateHandle[COUNT_KEY])
    }

    private fun ViewModelStoreProvider.owner(
        key: String,
        savedStateRegistryOwner: SavedStateRegistryOwner,
        factory: ViewModelProvider.Factory,
    ) = getOrCreateOwner(
        key = key,
        savedStateRegistryOwner = savedStateRegistryOwner,
        defaultFactory = factory,
        defaultCreationExtras = MutableCreationExtras(),
    )

    private class TestFactory(
        private val markerKey: CreationExtras.Key<String>,
    ) : ViewModelProvider.Factory {
        var receivedMarker: String? = null

        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            receivedMarker = extras[markerKey]
            @Suppress("UNCHECKED_CAST")
            return TestSavedStateViewModel(extras.createSavedStateHandle()) as T
        }
    }

    private class TestSavedStateViewModel(
        val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        init {
            if (!savedStateHandle.contains(COUNT_KEY)) savedStateHandle[COUNT_KEY] = 0
        }
    }

    private class TestSavedStateRegistryOwner(
        restoredState: SavedState? = null,
    ) : SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry.createUnsafe(this)
        private val controller = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry = controller.savedStateRegistry

        init {
            controller.performAttach()
            controller.performRestore(restoredState)
        }

        fun moveToCreated() {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        fun save(): SavedState = savedState().also(controller::performSave)
    }

    private companion object {
        const val METRO_MARKER = "metro-factory"
        const val COUNT_KEY = "count"
        const val RESTORED_COUNT = 42
    }
}
