/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.feature.analytics

import android.app.Activity
import android.content.Context
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.feature.analytics.model.AnalyticsSettings
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AnonymousAnalyticsManagerTest {
    private val dispatcher = StandardTestDispatcher()

    @Test
    fun givenAnonymousAnalyticsManager_whenInitializing_thenAnalyticsImplementationIsConfiguredCorrectly() = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement()
            .withAnonymousAnalyticsRecorderConfigure()
            .arrange()

        arrangement.toggleIsEnabledFlow(true)

        // when
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            isEnabledFlow = arrangement.isEnabledFlow.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            dispatcher = dispatcher
        )
        advanceUntilIdle()

        // then
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsRecorder.configure(
                arrangement.context,
                Arrangement.analyticsSettings
            )
        }
    }

    @Test
    fun givenIsEnabledFlowIsTrue_whenSendingAnEvent_thenEventIsSent() = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement()
            .withAnonymousAnalyticsRecorderConfigure()
            .arrange()

        val event = Arrangement.Companion.DummyEvent(
            key = "key1",
            attribute1 = "attr1"
        )

        arrangement.toggleIsEnabledFlow(true)

        // when
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            isEnabledFlow = arrangement.isEnabledFlow.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            dispatcher = dispatcher
        )
        advanceUntilIdle()

        manager.sendEvent(event)

        // then
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsRecorder.sendEvent(event)
        }
    }

    @Test
    fun givenIsEnabledFlowIsTrue_whenSettingToFalseAndSendingEvent_thenNoEventsAreSent() = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement()
            .withAnonymousAnalyticsRecorderConfigure()
            .arrange()

        val event = Arrangement.Companion.DummyEvent(
            key = "key1",
            attribute1 = "attr1"
        )

        arrangement.toggleIsEnabledFlow(true)

        // when
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            isEnabledFlow = arrangement.isEnabledFlow.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            dispatcher = dispatcher
        )
        advanceUntilIdle()

        manager.sendEvent(event)

        arrangement.toggleIsEnabledFlow(false)
        advanceUntilIdle()

        manager.sendEvent(event)

        // then
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsRecorder.sendEvent(event)
        }
    }

    @Test
    fun givenIsEnabledFlowIsFalse_whenCallingOnStart_thenRecorderOnStartIsNotCalled() = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement()
            .withAnonymousAnalyticsRecorderConfigure()
            .arrange()

        arrangement.toggleIsEnabledFlow(false)
        advanceUntilIdle()

        manager.onStart(activity = mockk<Activity>())

        // then
        verify(exactly = 0) {
            arrangement.anonymousAnalyticsRecorder.onStart(any())
        }
    }

    @Test
    fun givenIsEnabledFlowIsFalse_whenCallingOnStop_thenRecorderOnStopIsNotCalled() = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement()
            .withAnonymousAnalyticsRecorderConfigure()
            .arrange()

        arrangement.toggleIsEnabledFlow(false)
        advanceUntilIdle()

        manager.onStop(activity = mockk<Activity>())

        // then
        verify(exactly = 0) {
            arrangement.anonymousAnalyticsRecorder.onStop()
        }
    }

    @Test
    fun givenIsEnabledFlowIsFalseAndOneActivityStarted_whenTogglingEnabledToTrue_thenCallStartAfterToggled() = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement()
            .withAnonymousAnalyticsRecorderConfigure()
            .toggleIsEnabledFlow(false)
            .arrange()
        val activity: Activity = mockk()
        manager.onStart(activity)
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            isEnabledFlow = arrangement.isEnabledFlow.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            dispatcher = dispatcher
        )
        advanceUntilIdle()
        verify(exactly = 0) {
            arrangement.anonymousAnalyticsRecorder.onStart(activity)
        }

        // when
        arrangement.toggleIsEnabledFlow(true)
        advanceUntilIdle()

        // then
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsRecorder.onStart(activity)
        }
    }

    @Test
    fun givenManagerInitialized_whenTogglingEnabledToFalse_thenHaltIsCalled() = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement()
            .withAnonymousAnalyticsRecorderConfigure()
            .arrange()
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            isEnabledFlow = arrangement.isEnabledFlow.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            dispatcher = dispatcher
        )
        advanceUntilIdle()

        // when
        arrangement.toggleIsEnabledFlow(false)
        advanceUntilIdle()

        // then
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsRecorder.halt()
        }
    }

    private class Arrangement {
        @MockK
        lateinit var context: Context

        @MockK
        lateinit var anonymousAnalyticsRecorder: AnonymousAnalyticsRecorder

        val isEnabledFlow = Channel<Boolean>(capacity = Channel.UNLIMITED)

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        private val manager by lazy {
            AnonymousAnalyticsManagerImpl
        }

        fun arrange() = this to manager

        fun withAnonymousAnalyticsRecorderConfigure() = apply {
            every { anonymousAnalyticsRecorder.configure(any(), any()) } returns Unit
        }

        suspend fun toggleIsEnabledFlow(enabled: Boolean) = apply {
            isEnabledFlow.send(enabled)
        }

        companion object {
            val analyticsSettings = AnalyticsSettings(
                countlyAppKey = "appKey",
                countlyServerUrl = "serverUrl",
                enableDebugLogging = true
            )
            data class DummyEvent(
                override val key: String,
                val attribute1: String
            ) : AnalyticsEvent {
                override fun toSegmentation(): Map<String, Any> = mapOf(
                    "attribute1" to attribute1
                )
            }
        }
    }
}
