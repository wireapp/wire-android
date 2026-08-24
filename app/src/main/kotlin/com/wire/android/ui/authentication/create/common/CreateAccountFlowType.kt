/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.authentication.create.common

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import kotlinx.parcelize.Parcelize

@Parcelize
enum class CreateAccountFlowType(
    val routeArg: String,
    @StringRes val titleResId: Int,
    val overviewResources: OverviewResources,
    val emailResources: EmailResources,
) : Parcelable {
    CreatePersonalAccount(
        routeArg = "create_personal_account",
        titleResId = AuthenticationR.string.create_personal_account_title,
        overviewResources = OverviewResources(
            overviewContentTitleResId = null,
            overviewContentTextResId = AuthenticationR.string.create_personal_account_text,
            overviewContentIconResId = AuthenticationR.drawable.ic_create_personal_account,
            overviewLearnMoreTextResId = R.string.label_learn_more
        ),
        emailResources = EmailResources(
            emailSubtitleResId = AuthenticationR.string.create_personal_account_email_text
        ),
    ),
    CreateTeam(
        routeArg = "create_team",
        titleResId = R.string.create_team_title,
        overviewResources = OverviewResources(
            overviewContentTitleResId = AuthenticationR.string.create_team_content_title,
            overviewContentTextResId = AuthenticationR.string.create_team_text,
            overviewContentIconResId = AuthenticationR.drawable.ic_create_team,
            overviewLearnMoreTextResId = AuthenticationR.string.create_team_learn_more
        ),
        emailResources = EmailResources(
            emailSubtitleResId = AuthenticationR.string.create_team_email_text
        ),
    );

    companion object {
        fun fromRouteArg(routeArg: String?) = values().firstOrNull { it.routeArg == routeArg }
    }
}

@Parcelize
data class OverviewResources(
    @StringRes val overviewContentTitleResId: Int?,
    @StringRes val overviewContentTextResId: Int,
    @DrawableRes val overviewContentIconResId: Int,
    @StringRes val overviewLearnMoreTextResId: Int
) : Parcelable

@Parcelize
data class EmailResources(@StringRes val emailSubtitleResId: Int) : Parcelable
