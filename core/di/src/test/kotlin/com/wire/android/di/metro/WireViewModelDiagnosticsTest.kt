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

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WireViewModelDiagnosticsTest {
    private val installations = mutableListOf<AutoCloseable>()

    @AfterEach
    fun tearDown() {
        installations.reversed().forEach(AutoCloseable::close)
    }

    @Test
    fun givenSinkIsInstalled_whenRuntimeEventsOccur_thenOwnerAndViewModelIdentityAreForwarded() {
        val events = mutableListOf<String>()
        val sink = RecordingSink(events)
        val owner = TestOwner()
        installations += WireViewModelDiagnostics.install(sink)

        WireViewModelDiagnostics.ownerAvailable(owner, "flow:login")
        WireViewModelDiagnostics.viewModelRequested(owner, "com.wire.LoginViewModel")
        WireViewModelDiagnostics.ownerReleased(owner, "flow:login")
        WireViewModelDiagnostics.ownerCleared("flow:login")

        assertEquals(
            listOf(
                "available:flow:login",
                "viewModel:com.wire.LoginViewModel",
                "released:flow:login",
                "cleared:flow:login",
            ),
            events,
        )
    }

    @Test
    fun givenSinkWasReplaced_whenOldInstallationCloses_thenCurrentSinkRemainsInstalled() {
        val oldEvents = mutableListOf<String>()
        val currentEvents = mutableListOf<String>()
        val oldInstallation =
            WireViewModelDiagnostics.install(RecordingSink(oldEvents))
        installations += oldInstallation
        installations += WireViewModelDiagnostics.install(RecordingSink(currentEvents))

        oldInstallation.close()
        WireViewModelDiagnostics.ownerCleared("flow:login")

        assertEquals(emptyList<String>(), oldEvents)
        assertEquals(listOf("cleared:flow:login"), currentEvents)
    }

    private class TestOwner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    private class RecordingSink(
        private val events: MutableList<String>,
    ) : WireViewModelDiagnosticSink {
        override fun ownerAvailable(owner: ViewModelStoreOwner, ownerKey: String) {
            events += "available:$ownerKey"
        }

        override fun ownerReleased(owner: ViewModelStoreOwner, ownerKey: String) {
            events += "released:$ownerKey"
        }

        override fun ownerCleared(ownerKey: String) {
            events += "cleared:$ownerKey"
        }

        override fun viewModelRequested(owner: ViewModelStoreOwner, viewModelKey: String) {
            events += "viewModel:$viewModelKey"
        }
    }
}
