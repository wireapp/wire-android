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

package com.wire.navigation

import kotlinx.serialization.Serializable

/**
 * Stable, platform-independent identity of a ViewModel lifetime.
 */
@Serializable
sealed interface WireViewModelOwner {

    /**
     * Owns state for one concrete navigation entry.
     */
    @Serializable
    data class Entry(val entryId: WireNavEntryId) : WireViewModelOwner

    /**
     * Owns state shared by all entries in one explicitly identified flow.
     */
    @Serializable
    data class Flow(val flowId: String) : WireViewModelOwner {
        init {
            require(flowId.isNotBlank()) { "A ViewModel flow owner id cannot be blank" }
        }
    }

    /**
     * Owns state deliberately shared by routes for one explicit account session.
     */
    @Serializable
    data class Session(val sessionId: WireSessionId) : WireViewModelOwner

    /**
     * Owns process-wide state.
     */
    @Serializable
    data object Application : WireViewModelOwner
}

/**
 * Returns this route's default, entry-scoped ViewModel owner.
 */
fun WireRoute.entryViewModelOwner(): WireViewModelOwner.Entry =
    WireViewModelOwner.Entry(entryId)

/**
 * Returns the shared owners that a ViewModel hosted by this route may explicitly request.
 *
 * Merely making an owner available never changes the route's default [entryViewModelOwner].
 */
fun WireRoute.availableSharedViewModelOwners(): Set<WireViewModelOwner> =
    buildSet {
        flowId?.let { add(WireViewModelOwner.Flow(it)) }
        if (this@availableSharedViewModelOwners is SessionRoute) {
            add(WireViewModelOwner.Session(sessionId))
        }
        add(WireViewModelOwner.Application)
    }

/**
 * Returns the stable value key used by the platform runtime to retain an owner.
 */
fun WireViewModelOwner.stableKey(): String =
    when (this) {
        is WireViewModelOwner.Entry -> "entry:${entryId.value}"
        is WireViewModelOwner.Flow -> "flow:$flowId"
        is WireViewModelOwner.Session -> "session:${sessionId.value}@${sessionId.domain}"
        WireViewModelOwner.Application -> "application"
    }
