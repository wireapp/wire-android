package com.wire.android.ui.home.settings.account

import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.NavigationItem
import com.wire.android.util.ui.UIText

sealed class AccountDetailsItem(
    val title: UIText.StringResource,
    open val text: String,
    val navigationItem: NavigationItem,
    val clickable: Clickable
) {

    data class DisplayName(override val text: String) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_display_name),
        text = text,
        navigationItem = NavigationItem.Debug, // todo: replace later when implementing edit of field
        clickable = Clickable(enabled = false) {} // todo: replace later when implementing edit of field
    )

    data class Username(override val text: String) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_username),
        text = text,
        navigationItem = NavigationItem.Debug,// todo: replace later when implementing edit of field
        clickable = Clickable(enabled = false) {} // todo: replace later when implementing edit of field
    )

    data class Email(override val text: String) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_email),
        text = text,
        navigationItem = NavigationItem.Debug,// todo: replace later when implementing edit of field
        clickable = Clickable(enabled = false) {} // todo: replace later when implementing edit of field
    )

    data class Team(override val text: String) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_team),
        text = text,
        navigationItem = NavigationItem.Debug,// todo: replace later when implementing edit of field
        clickable = Clickable(enabled = false) {}
    )

    data class Domain(override val text: String) : AccountDetailsItem(
        title = UIText.StringResource(R.string.settings_myaccount_domain),
        text = text,
        navigationItem = NavigationItem.Debug,// todo: replace later when implementing edit of field
        clickable = Clickable(enabled = false) {}
    )

}
