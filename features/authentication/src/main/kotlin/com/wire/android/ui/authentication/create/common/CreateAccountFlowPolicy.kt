package com.wire.android.ui.authentication.create.common

import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType

/** Route-stable semantic policy shared by every new create-account step. */
data class CreateAccountFlowPolicy(
    val isTeam: Boolean,
    val usesPersonalOverview: Boolean,
)

fun CreateAccountRouteFlowType.createAccountFlowPolicy(): CreateAccountFlowPolicy = when (this) {
    CreateAccountRouteFlowType.PERSONAL -> CreateAccountFlowPolicy(isTeam = false, usesPersonalOverview = true)
    CreateAccountRouteFlowType.TEAM -> CreateAccountFlowPolicy(isTeam = true, usesPersonalOverview = false)
}
