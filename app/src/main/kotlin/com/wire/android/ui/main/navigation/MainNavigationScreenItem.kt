package com.wire.android.ui.main.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.wire.android.R
import com.wire.android.ui.main.archive.ArchiveScreen
import com.wire.android.ui.main.convesations.ConversationsScreen
import com.wire.android.ui.main.settings.SettingsScreen
import com.wire.android.ui.main.support.SupportScreen
import com.wire.android.ui.main.vault.VaultScreen
import com.wire.android.ui.main.userprofile.UserProfileScreen

enum class MainNavigationScreenItem(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    val route: String,
    val content: @Composable (NavBackStackEntry) -> Unit
) {
    Conversations(
        R.drawable.ic_conversation,
        R.string.conversations_screen_title,
        "conversations",
        { ConversationsScreen() }
    ),

    Vault(
        R.drawable.ic_vault,
        R.string.vault_screen_title,
        "vault",
        { VaultScreen() }),

    Archive(
        R.drawable.ic_archive,
        R.string.archive_screen_title,
        "archive",
        { ArchiveScreen() }),

    UserProfile(
        R.drawable.ic_archive,
        R.string.user_profile_screen_title,
        "user_profile",
        { UserProfileScreen() }),

    Settings(
        R.drawable.ic_settings,
        R.string.settings_screen_title,
        "settings",
        { SettingsScreen() }),

    Support(
        R.drawable.ic_support,
        R.string.support_screen_title,
        "support",
        { SupportScreen() }
    );

    companion object {
        private val map = values().associateBy(MainNavigationScreenItem::route)
        fun fromRoute(route: String?): MainNavigationScreenItem? = map[route]
    }
}
