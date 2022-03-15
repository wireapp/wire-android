package com.wire.android.ui.authentication.create.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wire.android.R

enum class CreateAccountFlowType(
    val routeArg: String,
    @StringRes val titleResId: Int,
    val overviewResources: OverviewResources,
    val emailResources: EmailResources,
    val summaryResources: SummaryResources
) {
    CreatePersonalAccount(
        routeArg =  "create_personal_account",
        titleResId = R.string.create_personal_account_title,
        overviewResources = OverviewResources(
            overviewContentTitleResId = null,
            overviewContentTextResId = R.string.create_personal_account_text,
            overviewContentIconResId = R.drawable.ic_create_personal_account,
            overviewLearnMoreTextResId = R.string.label_learn_more
        ),
        emailResources = EmailResources(
            emailSubtitleResId = R.string.create_personal_account_email_text
        ),
        summaryResources = SummaryResources(
            summaryTextResId = R.string.create_personal_account_summary_text,
            summaryIconResId = R.drawable.ic_create_personal_account_success
        )
    ),
    CreateTeam(
        routeArg = "create_team",
        titleResId = R.string.create_team_title,
        overviewResources = OverviewResources(
            overviewContentTitleResId = R.string.create_team_content_title,
            overviewContentTextResId = R.string.create_team_text,
            overviewContentIconResId = R.drawable.ic_create_team,
            overviewLearnMoreTextResId = R.string.create_team_learn_more
        ),
        emailResources = EmailResources(
            emailSubtitleResId = R.string.create_team_email_text
        ),
        summaryResources = SummaryResources(
            summaryTextResId = R.string.create_team_summary_text,
            summaryIconResId = R.drawable.ic_create_team_success
        )
    );
    companion object {
        fun fromRouteArg(routeArg: String?) = values().firstOrNull { it.routeArg == routeArg }
    }
}

data class OverviewResources(
    @StringRes val overviewContentTitleResId: Int?,
    @StringRes val overviewContentTextResId: Int,
    @DrawableRes val overviewContentIconResId: Int,
    @StringRes val overviewLearnMoreTextResId: Int
)
data class SummaryResources(
    @StringRes val summaryTextResId: Int,
    @DrawableRes val summaryIconResId: Int
)
data class EmailResources(@StringRes val emailSubtitleResId: Int)


enum class CreateAccountUsernameFlowType(
    val routeArg: String = "",
    @StringRes val titleResId: Int
) {
    CreatePersonalAccount("create_personal_account", R.string.create_personal_account_title),
    CreateTeam("create_team", R.string.create_team_title),
    AppStart("", R.string.set_username_title);
    companion object {
        fun fromRouteArg(routeArg: String?) = values().firstOrNull { it.routeArg == routeArg }
    }
}
