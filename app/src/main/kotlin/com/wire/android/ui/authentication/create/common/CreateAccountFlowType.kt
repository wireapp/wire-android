package com.wire.android.ui.authentication.create.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wire.android.R

enum class CreateAccountFlowType(
    val routeArg: String,
    @StringRes val titleResId: Int,
    @StringRes val overviewContentTitleResId: Int?,
    @StringRes val overviewContentTextResId: Int,
    @DrawableRes val overviewContentIconResId: Int,
    @StringRes val overviewLearnMoreTextResId: Int,
    @StringRes val emailSubtitleResId: Int,
    @StringRes val summaryTextResId: Int,
    @DrawableRes val summaryIconResId: Int,
) {
    CreatePersonalAccount(
        routeArg =  "create_personal_account",
        titleResId = R.string.create_personal_account_title,
        overviewContentTitleResId = null,
        overviewContentTextResId = R.string.create_personal_account_text,
        overviewContentIconResId = R.drawable.ic_create_personal_account,
        overviewLearnMoreTextResId = R.string.label_learn_more,
        emailSubtitleResId = R.string.create_personal_account_email_text,
        summaryTextResId = R.string.create_personal_account_summary_text,
        summaryIconResId = R.drawable.ic_create_personal_account_success
    ),
    CreateTeam(
        routeArg = "create_team",
        titleResId = R.string.create_team_title,
        overviewContentTitleResId = R.string.create_team_content_title,
        overviewContentTextResId = R.string.create_team_text,
        overviewContentIconResId = R.drawable.ic_create_team,
        overviewLearnMoreTextResId = R.string.create_team_learn_more,
        emailSubtitleResId = R.string.create_team_email_text,
        summaryTextResId = R.string.create_team_summary_text,
        summaryIconResId = R.drawable.ic_create_team_success
    );
    companion object {
        fun fromRouteArg(routeArg: String?) = values().firstOrNull { it.routeArg == routeArg }
    }
}

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
