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

package com.wire.android.ui.home.settings.multifactorauthentication

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultContractId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

@Serializable
enum class MultiFactorAuthenticationSetupStep {
    CONFIRM_PASSWORD,
    LINK_AUTHENTICATOR,
    MANUAL_KEY,
    VERIFY_CODE,
    RECOVERY_CODES,
    NAME_APP,
}

/** Only navigation identity is saved; enrollment secrets stay in the flow ViewModel. */
@Serializable
data class MultiFactorAuthenticationSetupRoute(
    override val sessionId: WireSessionId,
    val step: MultiFactorAuthenticationSetupStep = MultiFactorAuthenticationSetupStep.CONFIRM_PASSWORD,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
    override val flowId: String = "multi-factor-authentication-setup:${entryId.value}",
) : SessionRoute {
    override val routeId: String
        get() = "app/multi_factor_authentication_setup/${step.name.lowercase()}"

    init {
        require(step != MultiFactorAuthenticationSetupStep.NAME_APP)
    }
}

@Serializable
data class MultiFactorAuthenticationSetupResult(
    val authenticatorId: String,
    val authenticatorName: String,
    val recoveryCodesCount: Int,
)

internal val MultiFactorAuthenticationSetupResultContract =
    WireNavResultContract<MultiFactorAuthenticationSetupResult>(
        WireNavResultContractId("settings.multi-factor-authentication-setup")
    )
