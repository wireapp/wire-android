package com.wire.android.ui.authentication.create.common

import com.wire.android.feature.authentication.R
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType

/** Route-stable semantic policy shared by every new create-account step. */
data class CreateAccountFlowPolicy(
    val isTeam: Boolean,
    val titleResId: Int,
    val emailSubtitleResId: Int,
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
        titleResId = R.string.create_personal_account_title,
        emailSubtitleResId = R.string.create_personal_account_email_text,
        overview = CreateAccountOverviewPolicy(
            contentTitleResId = null,
            contentTextResId = R.string.create_personal_account_text,
            contentIconResId = R.drawable.ic_create_personal_account,
            learnMoreTextResId = null,
        ),
    )
    CreateAccountRouteFlowType.TEAM -> CreateAccountFlowPolicy(
        isTeam = true,
        titleResId = R.string.create_team_title,
        emailSubtitleResId = R.string.create_team_email_text,
        overview = CreateAccountOverviewPolicy(
            R.string.create_team_content_title,
            R.string.create_team_text,
            R.drawable.ic_create_team,
            R.string.create_team_learn_more,
        ),
    )
}
