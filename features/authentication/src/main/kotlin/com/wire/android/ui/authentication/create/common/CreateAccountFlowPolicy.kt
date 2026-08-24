package com.wire.android.ui.authentication.create.common

import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.feature.authentication.R

/** Route-stable semantic policy shared by every new create-account step. */
data class CreateAccountFlowPolicy(
    val isTeam: Boolean,
    val overview: CreateAccountOverviewPolicy,
)

data class CreateAccountOverviewPolicy(
    val contentTitleResId: Int?,
    val contentTextResId: Int,
    val contentIconResId: Int,
    val learnMoreTextResId: Int?,
)

fun CreateAccountRouteFlowType.createAccountFlowPolicy(): CreateAccountFlowPolicy = when (this) {
    CreateAccountRouteFlowType.PERSONAL -> CreateAccountFlowPolicy(
        isTeam = false,
        overview = CreateAccountOverviewPolicy(null, R.string.create_personal_account_text, R.drawable.ic_create_personal_account, null),
    )
    CreateAccountRouteFlowType.TEAM -> CreateAccountFlowPolicy(
        isTeam = true,
        overview = CreateAccountOverviewPolicy(
            R.string.create_team_content_title,
            R.string.create_team_text,
            R.drawable.ic_create_team,
            R.string.create_team_learn_more,
        ),
    )
}
