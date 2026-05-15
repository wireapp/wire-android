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
package com.wire.ios.shared.auth.newlogin

fun NewLoginIdentifierState.withUserIdentifier(userIdentifier: String): NewLoginIdentifierState {
    val newFlowState = when (flowState) {
        is NewLoginIdentifierFlowState.TextFieldError -> NewLoginIdentifierFlowState.Default
        else -> flowState
    }
    return copy(
        userIdentifier = userIdentifier,
        flowState = newFlowState,
        nextEnabled = newFlowState !is NewLoginIdentifierFlowState.Loading && userIdentifier.isNotEmpty(),
    )
}

fun NewLoginIdentifierState.withFlowState(flowState: NewLoginIdentifierFlowState): NewLoginIdentifierState =
    copy(
        flowState = flowState,
        nextEnabled = flowState !is NewLoginIdentifierFlowState.Loading && userIdentifier.isNotEmpty(),
    )
