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

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** A fake admission boundary for Android operator tests, not a model of the real SQL/credential producer. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class FakeSessionAdmission(
    val coreLogic: CoreLogic = mockk(),
    private val initiallyReady: (UserId) -> UserSessionScope? = { null },
) {
    private data class Publication(val revision: Int, val session: UserSessionScope?)

    private val publications = mutableMapOf<UserId, MutableStateFlow<Publication>>()
    val attached = mutableListOf<UserSessionScope>()
    val drained = mutableListOf<UserSessionScope>()

    init {
        every { coreLogic.observeValidSession<Any>(any(), any()) } answers {
            val publication = state(firstArg())
            val observe = secondArg<suspend UserSessionScope.() -> Flow<Any>>()
            publication.flatMapLatest { available ->
                val session = available.session
                if (session == null) {
                    flowOf(null)
                } else {
                    flow<Any?> {
                        attached += session
                        try {
                            emitAll(observe(session))
                        } finally {
                            drained += session
                        }
                    }
                }
            }
        }
    }

    fun publish(userId: UserId, session: UserSessionScope?) {
        val state = state(userId)
        state.value = Publication(state.value.revision + 1, session)
    }

    private fun state(userId: UserId) = publications.getOrPut(userId) { MutableStateFlow(Publication(0, initiallyReady(userId))) }
}
