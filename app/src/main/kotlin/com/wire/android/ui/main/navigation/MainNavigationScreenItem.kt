package com.wire.android.ui.main.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wire.android.R


enum class MainNavigationScreenItem(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    val route: String,
    val hasSearchableTopBar: Boolean,
) {
    Conversations(
        icon = R.drawable.ic_conversation,
        title = R.string.conversations_screen_title,
        route = "conversations",
        hasSearchableTopBar = true
    ),

    Vault(
        icon = R.drawable.ic_vault,
        title = R.string.vault_screen_title,
        route = "vault",
        hasSearchableTopBar = true
    ),

    Archive(
        icon = R.drawable.ic_archive,
        title = R.string.archive_screen_title,
        route = "archive",
        hasSearchableTopBar = true
    ),

    UserProfile(
        icon = R.drawable.ic_launcher_foreground,
        title = R.string.user_profile_screen_title,
        route = "user_profile",
        hasSearchableTopBar = false
    ),

    Settings(
        icon = R.drawable.ic_settings,
        title = R.string.settings_screen_title,
        route = "settings",
        hasSearchableTopBar = false
    ),

    Support(
        icon = R.drawable.ic_support,
        title = R.string.support_screen_title,
        route = "support",
        hasSearchableTopBar = false
    );

    companion object {
        private val map = values().associateBy(MainNavigationScreenItem::route)
        fun fromRoute(route: String?): MainNavigationScreenItem? = map[route]
    }
}
