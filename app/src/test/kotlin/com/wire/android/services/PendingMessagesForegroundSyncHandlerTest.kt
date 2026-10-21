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
package com.wire.android.services

import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PendingMessagesForegroundSyncHandlerTest {

    @Test
    fun `given multiple valid sessions when sending pending messages then checks validity before sending for each user`() = runTest {
        val events = mutableListOf<String>()
        val handler = PendingMessagesForegroundSyncHandler(
            allSessions = {
                GetAllSessionsResult.Success(
                    listOf(
                        AccountInfo.Valid(FIRST_USER_ID),
                        AccountInfo.Valid(SECOND_USER_ID),
                    )
                )
            },
            doesValidSessionExist = {
                events.add("check:${it.value}")
                DoesValidSessionExistResult.Success(true)
            },
            sendPendingMessagesAfterForegroundSync = {
                events.add("send:${it.value}")
            }
        )

        handler.sendPendingMessagesForAllValidSessions()

        assertEquals(4, events.size)
        listOf(FIRST_USER_ID, SECOND_USER_ID).forEach { userId ->
            assertEquals(listOf("check:${userId.value}", "send:${userId.value}"), events.filter { it.endsWith(userId.value) })
        }
    }

    @Test
    fun `given duplicate and invalid sessions when sending pending messages then sends once per valid user`() = runTest {
        val sentPendingMessagesFor = mutableListOf<UserId>()
        val handler = PendingMessagesForegroundSyncHandler(
            allSessions = {
                GetAllSessionsResult.Success(
                    listOf(
                        AccountInfo.Valid(FIRST_USER_ID),
                        AccountInfo.Invalid(THIRD_USER_ID, LogoutReason.SELF_SOFT_LOGOUT),
                        AccountInfo.Valid(FIRST_USER_ID),
                        AccountInfo.Valid(SECOND_USER_ID),
                    )
                )
            },
            doesValidSessionExist = { DoesValidSessionExistResult.Success(true) },
            sendPendingMessagesAfterForegroundSync = { sentPendingMessagesFor.add(it) }
        )

        handler.sendPendingMessagesForAllValidSessions()

        assertEquals(listOf(FIRST_USER_ID, SECOND_USER_ID), sentPendingMessagesFor)
    }

    @Test
    fun `given user logs out before sending when sending pending messages then skips that user`() = runTest {
        val sentPendingMessagesFor = mutableListOf<UserId>()
        val handler = PendingMessagesForegroundSyncHandler(
            allSessions = {
                GetAllSessionsResult.Success(
                    listOf(
                        AccountInfo.Valid(FIRST_USER_ID),
                        AccountInfo.Valid(SECOND_USER_ID),
                    )
                )
            },
            doesValidSessionExist = {
                DoesValidSessionExistResult.Success(it != FIRST_USER_ID)
            },
            sendPendingMessagesAfterForegroundSync = { sentPendingMessagesFor.add(it) }
        )

        handler.sendPendingMessagesForAllValidSessions()

        assertEquals(listOf(SECOND_USER_ID), sentPendingMessagesFor)
    }

    @Test
    fun `given sending fails for one user when sending pending messages then continues with remaining users`() = runTest {
        val sentPendingMessagesFor = mutableListOf<UserId>()
        val handler = PendingMessagesForegroundSyncHandler(
            allSessions = {
                GetAllSessionsResult.Success(
                    listOf(
                        AccountInfo.Valid(FIRST_USER_ID),
                        AccountInfo.Valid(SECOND_USER_ID),
                    )
                )
            },
            doesValidSessionExist = { DoesValidSessionExistResult.Success(true) },
            sendPendingMessagesAfterForegroundSync = {
                if (it == FIRST_USER_ID) {
                    throw RuntimeException("backend failed")
                }
                sentPendingMessagesFor.add(it)
            }
        )

        handler.sendPendingMessagesForAllValidSessions()

        assertEquals(listOf(SECOND_USER_ID), sentPendingMessagesFor)
    }

    @Test
    fun `given valid session re-check fails for one user when sending pending messages then continues with remaining users`() = runTest {
        val sentPendingMessagesFor = mutableListOf<UserId>()
        val handler = PendingMessagesForegroundSyncHandler(
            allSessions = {
                GetAllSessionsResult.Success(
                    listOf(
                        AccountInfo.Valid(FIRST_USER_ID),
                        AccountInfo.Valid(SECOND_USER_ID),
                    )
                )
            },
            doesValidSessionExist = {
                if (it == FIRST_USER_ID) {
                    DoesValidSessionExistResult.Failure.Generic(CoreFailure.Unknown(null))
                } else {
                    DoesValidSessionExistResult.Success(true)
                }
            },
            sendPendingMessagesAfterForegroundSync = { sentPendingMessagesFor.add(it) }
        )

        handler.sendPendingMessagesForAllValidSessions()

        assertEquals(listOf(SECOND_USER_ID), sentPendingMessagesFor)
    }

    @Test
    fun `given getting valid sessions fails when sending pending messages then sends nothing`() = runTest {
        val sentPendingMessagesFor = mutableListOf<UserId>()
        val handler = PendingMessagesForegroundSyncHandler(
            allSessions = { GetAllSessionsResult.Failure.Generic(CoreFailure.Unknown(null)) },
            doesValidSessionExist = { DoesValidSessionExistResult.Success(true) },
            sendPendingMessagesAfterForegroundSync = { sentPendingMessagesFor.add(it) }
        )

        handler.sendPendingMessagesForAllValidSessions()

        assertEquals(emptyList<UserId>(), sentPendingMessagesFor)
    }

    @Test
    fun `given no valid sessions when sending pending messages then sends nothing`() = runTest {
        val sentPendingMessagesFor = mutableListOf<UserId>()
        val handler = PendingMessagesForegroundSyncHandler(
            allSessions = {
                GetAllSessionsResult.Success(
                    listOf(
                        AccountInfo.Invalid(FIRST_USER_ID, LogoutReason.SELF_SOFT_LOGOUT),
                        AccountInfo.Invalid(SECOND_USER_ID, LogoutReason.SESSION_EXPIRED),
                    )
                )
            },
            doesValidSessionExist = { DoesValidSessionExistResult.Success(true) },
            sendPendingMessagesAfterForegroundSync = { sentPendingMessagesFor.add(it) }
        )

        handler.sendPendingMessagesForAllValidSessions()

        assertEquals(emptyList<UserId>(), sentPendingMessagesFor)
    }

    @Test
    fun `pending A does not block B and cancelling the batch drains its waiting child`() = runTest {
        val waiting = CompletableDeferred<Unit>()
        val drained = CompletableDeferred<Unit>()
        val sent = mutableListOf<UserId>()
        val handler = PendingMessagesForegroundSyncHandler(
            allSessions = {
                GetAllSessionsResult.Success(listOf(AccountInfo.Valid(FIRST_USER_ID), AccountInfo.Valid(SECOND_USER_ID)))
            },
            doesValidSessionExist = { DoesValidSessionExistResult.Success(true) },
            sendPendingMessagesAfterForegroundSync = {
                if (it == FIRST_USER_ID) {
                    waiting.complete(Unit)
                    try { awaitCancellation() } finally { drained.complete(Unit) }
                } else {
                    sent += it
                }
            }
        )
        val batch = async { handler.sendPendingMessagesForAllValidSessions() }
        waiting.await()
        runCurrent()
        assertEquals(listOf(SECOND_USER_ID), sent)
        assertTrue(batch.isActive)
        batch.cancel()
        batch.join()
        assertTrue(batch.isCancelled)
        assertTrue(drained.isCompleted)
    }

    @Test
    fun `send cancellation propagates from the concurrent batch`() = runTest {
        val cancellation = CancellationException("send cancelled")
        val handler = PendingMessagesForegroundSyncHandler(
            allSessions = { GetAllSessionsResult.Success(listOf(AccountInfo.Valid(FIRST_USER_ID))) },
            doesValidSessionExist = { DoesValidSessionExistResult.Success(true) },
            sendPendingMessagesAfterForegroundSync = { throw cancellation }
        )
        val observed = try {
            handler.sendPendingMessagesForAllValidSessions()
            error("expected cancellation")
        } catch (actual: CancellationException) {
            actual
        }
        assertEquals(cancellation.message, observed.message)
    }

    private companion object {
        private val FIRST_USER_ID = UserId("firstUser", "wire.com")
        private val SECOND_USER_ID = UserId("secondUser", "wire.com")
        private val THIRD_USER_ID = UserId("thirdUser", "wire.com")
    }
}
