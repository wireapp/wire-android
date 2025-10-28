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

package com.wire.android.ui.home.settings.account

import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.theme.Accent
import com.wire.android.util.ui.UIText

sealed class AccountDetailsItem(
    val title: UIText.StringResource,
    open val text: UIText,
    open val clickable: Clickable?
) {

    data class DisplayName(override val text: UIText, override val clickable: Clickable?) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_display_name),
        text = text,
        clickable = clickable
    )

    data class Username(override val text: UIText, override val clickable: Clickable?) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_username),
        text = text,
        clickable = clickable
    )

    data class Email(override val text: UIText, override val clickable: Clickable?) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_email),
        text = text,
        clickable = clickable
    )

    data class UserColor(override val text: UIText, override val clickable: Clickable?, val accent: Accent) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_user_color),
        text = text,
        clickable = clickable
    )

    data class Team(override val text: UIText) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_team),
        text = text,
        clickable = Clickable(enabled = false) {}
    )

    data class Domain(override val text: UIText) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_domain),
        text = text,
        clickable = Clickable(enabled = false) {}
    )
}
