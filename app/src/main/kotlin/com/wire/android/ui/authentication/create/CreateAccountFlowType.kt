package com.wire.android.ui.authentication.create

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wire.android.R

enum class CreateAccountFlowType(
    val routeArg: String = "",
    @StringRes val titleResId: Int,
    @StringRes val summaryTextResId: Int = 0,
    @DrawableRes val summaryIconResId: Int = 0,
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
        ),
    None(titleResId = R.string.set_username_title);

    companion object {
        fun fromString(type: String?) = values().firstOrNull { it.routeArg == type } ?: None
    }
}
