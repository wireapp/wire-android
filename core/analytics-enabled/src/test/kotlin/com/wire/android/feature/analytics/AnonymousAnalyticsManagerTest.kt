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
import com.wire.android.feature.analytics.handler.AnalyticsMigrationHandler
import com.wire.android.feature.analytics.handler.AnalyticsPropagationHandler
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.feature.analytics.model.AnalyticsProfileProperties
import com.wire.android.feature.analytics.model.AnalyticsResult
import com.wire.android.feature.analytics.model.AnalyticsSettings
import com.wire.kalium.logic.data.analytics.AnalyticsIdentifierResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
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

        arrangement.withAnalyticsResult(Arrangement.existingIdentifierResult)

        // when
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            analyticsResultFlow = arrangement.analyticsResultChannel.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            migrationHandler = arrangement.migrationHandler,
            propagationHandler = arrangement.propagationHandler,
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

        arrangement.withAnalyticsResult(Arrangement.existingIdentifierResult)

        // when
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            analyticsResultFlow = arrangement.analyticsResultChannel.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            migrationHandler = arrangement.migrationHandler,
            propagationHandler = arrangement.propagationHandler,
            dispatcher = dispatcher
        )
        advanceUntilIdle()

        manager.sendEvent(event)
        advanceUntilIdle()

        // then
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsRecorder.sendEvent(any())
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

        arrangement.withAnalyticsResult(Arrangement.existingIdentifierResult)

        // when
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            analyticsResultFlow = arrangement.analyticsResultChannel.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            migrationHandler = arrangement.migrationHandler,
            propagationHandler = arrangement.propagationHandler,
            dispatcher = dispatcher
        )

        manager.sendEvent(event)

        arrangement.withAnalyticsResult(Arrangement.disabledIdentifierResult)
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

        arrangement.withAnalyticsResult(Arrangement.disabledIdentifierResult)

        // when
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            analyticsResultFlow = arrangement.analyticsResultChannel.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            migrationHandler = arrangement.migrationHandler,
            propagationHandler = arrangement.propagationHandler,
            dispatcher = dispatcher
        )

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

        arrangement.withAnalyticsResult(Arrangement.disabledIdentifierResult)

        // when
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            analyticsResultFlow = arrangement.analyticsResultChannel.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            migrationHandler = arrangement.migrationHandler,
            propagationHandler = arrangement.propagationHandler,
            dispatcher = dispatcher
        )

        manager.onStop(activity = mockk<Activity>())
        advanceUntilIdle()

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
            .withAnalyticsResult(Arrangement.disabledIdentifierResult)
            .arrange()
        val activity: Activity = mockk()

        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            analyticsResultFlow = arrangement.analyticsResultChannel.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            migrationHandler = arrangement.migrationHandler,
            propagationHandler = arrangement.propagationHandler,
            dispatcher = dispatcher
        )

        manager.onStart(activity)

        verify(exactly = 0) {
            arrangement.anonymousAnalyticsRecorder.onStart(activity)
        }

        // when
        arrangement.withAnalyticsResult(Arrangement.existingIdentifierResult)
        advanceUntilIdle()

        // then
        verify {
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
            analyticsResultFlow = arrangement.analyticsResultChannel.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            migrationHandler = arrangement.migrationHandler,
            propagationHandler = arrangement.propagationHandler,
            dispatcher = dispatcher
        )

        // when
        arrangement.withAnalyticsResult(Arrangement.disabledIdentifierResult)
        advanceUntilIdle()

        // then
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsRecorder.halt()
        }
    }

    @Test
    fun givenManagerInitialized_whenRecordingView_thenScreenIsRecorded() = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement()
            .withAnonymousAnalyticsRecorderConfigure()
            .arrange()

        val screen = "screen"
        arrangement.withAnalyticsResult(Arrangement.existingIdentifierResult)

        // when
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            analyticsResultFlow = arrangement.analyticsResultChannel.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            migrationHandler = arrangement.migrationHandler,
            propagationHandler = arrangement.propagationHandler,
            dispatcher = dispatcher
        )
        advanceUntilIdle()

        manager.recordView(screen)
        advanceUntilIdle()

        // then
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsRecorder.recordView(any())
        }
    }

    @Test
    fun givenManagerInitialized_whenRecordingViewAndFlagDisabled_thenScreenIsNOTRecorded() = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement()
            .withAnonymousAnalyticsRecorderConfigure()
            .arrange(shouldTrackViews = false)

        val screen = "screen"
        arrangement.withAnalyticsResult(Arrangement.existingIdentifierResult)

        // when
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            analyticsResultFlow = arrangement.analyticsResultChannel.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            migrationHandler = arrangement.migrationHandler,
            propagationHandler = arrangement.propagationHandler,
            dispatcher = dispatcher
        )
        advanceUntilIdle()

        manager.recordView(screen)
        advanceUntilIdle()

        // then
        verify(exactly = 0) {
            arrangement.anonymousAnalyticsRecorder.recordView(eq(screen))
        }
    }

    @Test
    fun givenManagerInitialized_whenStoppingView_thenScreenIsStoppedToRecord() = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement()
            .withAnonymousAnalyticsRecorderConfigure()
            .arrange()

        val screen = "screen"
        arrangement.withAnalyticsResult(Arrangement.existingIdentifierResult)

        // when
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            analyticsResultFlow = arrangement.analyticsResultChannel.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            migrationHandler = arrangement.migrationHandler,
            propagationHandler = arrangement.propagationHandler,
            dispatcher = dispatcher
        )
        advanceUntilIdle()

        manager.stopView(screen)
        advanceUntilIdle()

        // then
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsRecorder.stopView(any())
        }
    }

    @Test
    fun givenManagerInitialized_whenApplicationCreated_thenApplicationOnCreateIsRecorded() = runTest(dispatcher) {
        // given
        val (arrangement, manager) = Arrangement()
            .withAnonymousAnalyticsRecorderConfigure()
            .arrange()

        arrangement.withAnalyticsResult(Arrangement.existingIdentifierResult)

        // when
        manager.init(
            context = arrangement.context,
            analyticsSettings = Arrangement.analyticsSettings,
            analyticsResultFlow = arrangement.analyticsResultChannel.consumeAsFlow(),
            anonymousAnalyticsRecorder = arrangement.anonymousAnalyticsRecorder,
            migrationHandler = arrangement.migrationHandler,
            propagationHandler = arrangement.propagationHandler,
            dispatcher = dispatcher
        )
        advanceUntilIdle()

        manager.applicationOnCreate()
        advanceUntilIdle()

        // then
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsRecorder.applicationOnCreate()
        }
    }

    private class Arrangement {
        @MockK
        lateinit var context: Context

        @MockK
        lateinit var anonymousAnalyticsRecorder: AnonymousAnalyticsRecorder

        @MockK
        lateinit var migrationHandler: AnalyticsMigrationHandler<DummyManager>

        @MockK
        lateinit var propagationHandler: AnalyticsPropagationHandler<DummyManager>

        val analyticsResultChannel = Channel<AnalyticsResult<DummyManager>>(capacity = Channel.UNLIMITED)

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            every { anonymousAnalyticsRecorder.onStop() } returns Unit
            every { anonymousAnalyticsRecorder.onStart(any()) } returns Unit
            every { anonymousAnalyticsRecorder.sendEvent(any()) } returns Unit
            every { anonymousAnalyticsRecorder.recordView(any()) } returns Unit
            every { anonymousAnalyticsRecorder.stopView(any()) } returns Unit
            every { anonymousAnalyticsRecorder.applicationOnCreate() } returns Unit
            coEvery { anonymousAnalyticsRecorder.setTrackingIdentifierWithMerge(any(), any(), any()) } returns Unit
            coEvery { anonymousAnalyticsRecorder.setTrackingIdentifierWithoutMerge(any(), any(), any(), any()) } returns Unit
        }

        private val manager by lazy {
            AnonymousAnalyticsManagerImpl
        }

        fun arrange(shouldTrackViews: Boolean = true) = this to manager.apply { VIEW_TRACKING_ENABLED = shouldTrackViews }

        fun withAnonymousAnalyticsRecorderConfigure() = apply {
            every { anonymousAnalyticsRecorder.configure(any(), any()) } returns Unit
        }

        suspend fun withAnalyticsResult(result: AnalyticsResult<DummyManager>) = apply {
            analyticsResultChannel.send(result)
        }

        companion object {
            const val CURRENT_IDENTIFIER = "abcd-1234"
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

            interface DummyManager

            private fun dummyManager() = object : DummyManager {}
            val existingIdentifierResult = AnalyticsResult<DummyManager>(
                identifierResult = AnalyticsIdentifierResult.ExistingIdentifier(CURRENT_IDENTIFIER),
                profileProperties = suspend {
                    AnalyticsProfileProperties(
                        isTeamMember = true,
                        teamId = null,
                        contactsAmount = null,
                        teamMembersAmount = null,
                        isEnterprise = null
                    )
                },
                manager = dummyManager()
            )
            val disabledIdentifierResult = existingIdentifierResult.copy(
                identifierResult = AnalyticsIdentifierResult.Disabled
            )
        }
    }
}
