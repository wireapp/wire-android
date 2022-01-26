package com.wire.android.ui.main.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import com.wire.android.R
import com.wire.android.ui.main.archive.ArchiveScreen
import com.wire.android.ui.main.convesations.ConversationsScreen
import com.wire.android.ui.main.settings.SettingsScreen
import com.wire.android.ui.main.support.SupportScreen
import com.wire.android.ui.main.userprofile.UserProfileScreen
import com.wire.android.ui.main.vault.VaultScreen

enum class MainNavigationScreenItem(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    val route: String,
    val hasSearchableTopBar: Boolean,
    val content: @Composable (NavBackStackEntry) -> Unit,
    val arguments: List<NamedNavArgument> = emptyList()
) {
    Conversations(
        icon = R.drawable.ic_conversation,
        title = R.string.conversations_screen_title,
        route = "conversations",
        hasSearchableTopBar = true,
        content = { ConversationsScreen(hiltViewModel()) }
    ),

    Vault(
        icon = R.drawable.ic_vault,
        title = R.string.vault_screen_title,
        route = "vault",
        hasSearchableTopBar = true,
        content = { VaultScreen() }),

    Archive(
        icon = R.drawable.ic_archive,
        title = R.string.archive_screen_title,
        route = "archive",
        hasSearchableTopBar = true,
        content = { ArchiveScreen() }),

    UserProfile(
        icon = R.drawable.ic_launcher_foreground,
        title = R.string.user_profile_screen_title,
        route = "user_profile",
        hasSearchableTopBar = false,
        content = { UserProfileScreen() }),

    Settings(
        icon = R.drawable.ic_settings,
        title = R.string.settings_screen_title,
        route = "settings",
        hasSearchableTopBar = false,
        content = { SettingsScreen() }),

    Support(
        icon = R.drawable.ic_support,
        title = R.string.support_screen_title,
        route = "support",
        hasSearchableTopBar = false,
        content = { SupportScreen() }
    );

    companion object {
        private val map = values().associateBy(MainNavigationScreenItem::route)
        fun fromRoute(route: String?): MainNavigationScreenItem? = map[route]
    }
}
