package com.wire.android.ui.authentication.create

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wire.android.R

enum class CreateAccountFlowType(
    val routeArg: String,
    @StringRes val titleResId: Int,
    @StringRes val summaryTextResId: Int,
    @DrawableRes val summaryIconResId: Int,
    ) {
    CreatePersonalAccount(
        routeArg =  "create_personal_account",
        titleResId = R.string.create_personal_account_title,
        summaryTextResId = R.string.create_personal_account_summary_text,
        summaryIconResId = R.drawable.ic_create_personal_account_success,
    ),
    CreateTeam(
        routeArg = "create_team",
        titleResId = R.string.create_personal_account_title, //TODO change
        summaryTextResId = R.string.create_personal_account_summary_text, //TODO change
        summaryIconResId = R.drawable.ic_create_personal_account_success, //TODO change
        );

    companion object {
        fun fromRouteArg(type: String?): CreateAccountFlowType? = values().firstOrNull { it.routeArg == type }
    }
}

enum class CreateAccountUsernameFlowType(
    val routeArg: String = "",
    @StringRes val titleResId: Int
) {
    CreatePersonalAccount("create_personal_account", R.string.create_personal_account_title),
    CreateTeam("create_team", R.string.create_personal_account_title),
    AppStart("", R.string.set_username_title);
    companion object {
        fun fromRouteArg(routeArg: String?) = values().firstOrNull { it.routeArg == routeArg }
    }
}
