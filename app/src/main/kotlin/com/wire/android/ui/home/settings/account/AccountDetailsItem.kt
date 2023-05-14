/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.settings.account

import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.NavigationItem
import com.wire.android.util.ui.UIText

sealed class AccountDetailsItem(
    val title: UIText.StringResource,
    open val text: String,
    val navigationItem: NavigationItem,
    open val clickable: Clickable?
) {

    data class DisplayName(override val text: String, override val clickable: Clickable?) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_display_name),
        text = text,
        navigationItem = NavigationItem.EditDisplayName,
        clickable = clickable
    )

    data class Username(override val text: String, override val clickable: Clickable?) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_username),
        text = text,
        navigationItem = NavigationItem.EditHandle,
        clickable = clickable
    )

    data class Email(override val text: String, override val clickable: Clickable?) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_email),
        text = text,
        navigationItem = NavigationItem.EditEmailAddress,
        clickable = clickable
    )

    data class Team(override val text: String) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_team),
        text = text,
        navigationItem = NavigationItem.Debug, // todo: replace later when implementing edit of field
        clickable = Clickable(enabled = false) {}
    )

    data class Domain(override val text: String) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_domain),
        text = text,
        navigationItem = NavigationItem.Debug, // todo: replace later when implementing edit of field
        clickable = Clickable(enabled = false) {}
    )
}
