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

package com.wire.android.session

import com.wire.android.framework.TestUser
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.call.CallsScope
import com.wire.kalium.logic.feature.call.usecase.SetBackgroundUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentSessionObserversTest {
    private val userId = TestUser.SELF_USER_ID
    private val valid = CurrentSessionResult.Success(AccountInfo.Valid(userId))

    @Test
    fun `Invalid during closing drains uploads and recent calls without cancelling their common parent`() = runTest {
        val admission = FakeSessionAdmission()
        val raw = MutableStateFlow<CurrentSessionResult>(valid)
        val session = uploadingSession()
        val recentCalls = MutableSharedFlow<com.wire.kalium.logic.data.call.RecentlyEndedCallMetadata>()
        coEvery { session.calls.observeRecentlyEndedCallMetadata() } returns recentCalls
        admission.publish(userId, session)
        val uploads = mutableListOf<Boolean>()
        val sibling = CompletableDeferred<Unit>()
        val parent = backgroundScope.launch {
            launch { admission.coreLogic.observeCurrentAssetUploadState(raw).collect { uploads += it } }
            launch { admission.coreLogic.observeCurrentRecentlyEndedCalls(raw).collect {} }
            launch { sibling.await() }
        }
        runCurrent()
        assertEquals(listOf(true), uploads)
        assertEquals(2, admission.attached.size)

        admission.publish(userId, null) // Provider closes before SQL publishes Invalid.
        raw.value = CurrentSessionResult.Success(AccountInfo.Invalid(userId, LogoutReason.SESSION_EXPIRED))
        runCurrent()

        assertEquals(false, uploads.last())
        assertEquals(2, admission.drained.size)
        assertEquals(2, admission.attached.size)
        assertTrue(parent.isActive)
        assertFalse(sibling.isCompleted)
        verify(exactly = 0) { admission.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun `early Valid waits for publication and same ID provider revision rebinds exactly once`() = runTest {
        val admission = FakeSessionAdmission()
        val raw = MutableStateFlow<CurrentSessionResult>(
            CurrentSessionResult.Success(AccountInfo.Invalid(userId, LogoutReason.SESSION_EXPIRED))
        )
        val uploads = mutableListOf<Boolean>()
        val old = uploadingSession()
        val replacement = uploadingSession()
        backgroundScope.launch { admission.coreLogic.observeCurrentAssetUploadState(raw).collect { uploads += it } }
        runCurrent()
        raw.value = valid // Retained-current metadata arrives before provider release.
        runCurrent()
        assertTrue(admission.attached.isEmpty())
        assertEquals(false, uploads.last())

        admission.publish(userId, old)
        runCurrent()
        assertEquals(listOf(old), admission.attached)
        assertEquals(true, uploads.last())

        admission.publish(userId, null)
        runCurrent()
        assertEquals(listOf(old), admission.drained)
        admission.publish(userId, replacement) // Raw StateFlow still holds exactly the same Valid instance.
        runCurrent()
        assertEquals(listOf(old, replacement), admission.attached)
        assertEquals(true, uploads.last())
        verify(exactly = 0) { admission.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun `delayed Valid snapshot cannot reopen a revoked generation`() = runTest {
        val admission = FakeSessionAdmission()
        val raw = MutableSharedFlow<CurrentSessionResult>()
        val outgoing = uploadingSession()
        admission.publish(userId, outgoing)
        val parent = backgroundScope.launch { admission.coreLogic.observeCurrentAssetUploadState(raw).collect {} }
        runCurrent()
        admission.publish(userId, null)
        raw.emit(valid)
        runCurrent()
        assertTrue(admission.attached.isEmpty())
        assertTrue(parent.isActive)
        verify(exactly = 0) { admission.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun `visibility replay uses captured session and preserves the fresh calls getter`() = runTest {
        val admission = FakeSessionAdmission()
        val session = mockk<UserSessionScope>()
        val calls = mockk<CallsScope>()
        val setBackground = mockk<SetBackgroundUseCase>()
        every { calls.setBackground } returns setBackground
        coEvery { setBackground(any()) } returns Unit
        var facadeReads = 0
        every { session.calls } answers {
            facadeReads++
            calls
        }
        admission.publish(userId, session)
        val visibility = MutableStateFlow(false)
        val accounts = flowOf(GetAllSessionsResult.Success(listOf(AccountInfo.Valid(userId))))
        val parent = backgroundScope.launch { admission.coreLogic.observeCallBackgroundState(accounts, visibility).collect {} }
        runCurrent()
        coVerify(exactly = 1) { setBackground(true) }
        visibility.value = true
        runCurrent()
        coVerify(exactly = 1) { setBackground(false) }
        assertEquals(2, facadeReads)

        admission.publish(userId, null)
        runCurrent()
        visibility.value = false
        runCurrent()
        assertEquals(2, facadeReads)
        coVerify(exactly = 1) { setBackground(true) }
        coVerify(exactly = 1) { setBackground(false) }
        assertEquals(1, admission.attached.size)
        assertEquals(listOf(session), admission.drained)
        assertTrue(parent.isActive)
        verify(exactly = 0) { admission.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun `one unavailable account does not block another accounts visibility`() = runTest {
        val admission = FakeSessionAdmission()
        val other = TestUser.OTHER_USER.id
        val session = mockk<UserSessionScope>(relaxed = true)
        admission.publish(other, session)
        val accounts = flowOf(GetAllSessionsResult.Success(listOf(AccountInfo.Valid(userId), AccountInfo.Valid(other))))
        backgroundScope.launch { admission.coreLogic.observeCallBackgroundState(accounts, flowOf(true)).collect {} }
        runCurrent()
        coVerify(exactly = 1) { session.calls.setBackground(false) }
        assertEquals(listOf(session), admission.attached)
    }

    @Test
    fun `genuine observer exception reaches the collector`() = runTest {
        val admission = FakeSessionAdmission()
        val session = uploadingSession()
        val failure = IllegalStateException("observer failure")
        coEvery { session.messages.observeAssetUploadState() } throws failure
        admission.publish(userId, session)
        val actual = try {
            admission.coreLogic.observeCurrentAssetUploadState(flowOf(valid)).collect {}
            error("expected observer failure")
        } catch (observed: IllegalStateException) {
            observed
        }
        assertSame(failure, generateSequence<Throwable>(actual) { it.cause }.last())
    }

    @Test
    fun `caller cancellation drains an attaching callback and does not become unavailable`() = runTest {
        val admission = FakeSessionAdmission()
        val session = uploadingSession()
        val entered = CompletableDeferred<Unit>()
        val drained = CompletableDeferred<Unit>()
        coEvery { session.messages.observeAssetUploadState() } coAnswers {
            entered.complete(Unit)
            try { awaitCancellation() } finally { drained.complete(Unit) }
        }
        admission.publish(userId, session)
        val result = async { admission.coreLogic.observeCurrentAssetUploadState(flowOf(valid)).collect {} }
        entered.await()
        result.cancel()
        result.join()
        assertTrue(result.isCancelled)
        assertTrue(drained.isCompleted)
    }

    @Test
    fun `one shot waits through unavailable while Valid but exits when raw account disappears`() = runTest {
        val admission = FakeSessionAdmission()
        val raw = MutableStateFlow<CurrentSessionResult>(valid)
        var actions = 0
        val pending = async {
            raw.observeCurrentValidSession(admission.coreLogic, waitForAdmission = true) {
                actions++
                flowOf(Unit)
            }.first()
        }
        runCurrent()
        assertTrue(pending.isActive)
        assertEquals(0, actions)
        raw.value = CurrentSessionResult.Failure.SessionNotFound
        runCurrent()
        assertEquals(null, pending.await())
        assertEquals(0, actions)
    }

    private fun uploadingSession(): UserSessionScope = mockk<UserSessionScope>(relaxed = true).also { session ->
        coEvery { session.messages.observeAssetUploadState() } returns MutableStateFlow(true)
        every { session.cells.uploadCoordinator.hasActiveUploads } returns MutableStateFlow(false)
    }
}
