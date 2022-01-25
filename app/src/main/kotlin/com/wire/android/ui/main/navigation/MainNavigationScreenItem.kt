package com.wire.android.ui.main.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wire.android.R


enum class MainNavigationScreenItem(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    val route: String,
) {
    Conversations(
        icon = R.drawable.ic_conversation,
        title = R.string.conversations_screen_title,
        route = "conversations",
    ),

    Vault(
        icon = R.drawable.ic_vault,
        title = R.string.vault_screen_title,
        route = "vault",
    ),

    Archive(
        icon = R.drawable.ic_archive,
        title = R.string.archive_screen_title,
        route = "archive",
    ),

    UserProfile(
        icon = R.drawable.ic_launcher_foreground,
        title = R.string.user_profile_screen_title,
        route = "user_profile",
    ),

    Settings(
        icon = R.drawable.ic_settings,
        title = R.string.settings_screen_title,
        route = "settings",
    ),

    Support(
        icon = R.drawable.ic_support,
        title = R.string.support_screen_title,
        route = "support",
    );

    companion object {
        private val map = values().associateBy(MainNavigationScreenItem::route)
        fun fromRoute(route: String?): MainNavigationScreenItem? = map[route]
    }
}
