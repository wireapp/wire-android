package com.wire.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import com.wire.android.R
import com.wire.android.ui.home.archive.ArchiveScreen
import com.wire.android.ui.home.conversationslist.ConversationListViewModel
import com.wire.android.ui.home.conversationslist.ConversationRouterHomeBridge
import com.wire.android.ui.home.vault.VaultScreen

@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
enum class HomeItem(
    @StringRes val title: Int,
    val isSearchable: Boolean = false,
    val content: @Composable (HomeUIState, HomeTabsViewModels) -> Unit,
) {
    Conversations(
        title = R.string.conversations_screen_title,
        isSearchable = true,
        content = { homeState, homeTabsViewModels ->
                ConversationRouterHomeBridge(
                    viewModel = homeTabsViewModels.conversationListViewModel,
                    onHomeBottomSheetContentChanged = homeState::changeBottomSheetContent,
                    onBottomSheetVisibilityChanged = homeState::toggleBottomSheetVisibility,
                    onScrollPositionProviderChanged = homeState::updateScrollPositionProvider,
                )
        }
    );
// TODO: Re-enable once we have vault
//    Vault(
//        title = R.string.vault_screen_title,
//        content = { _,_-> VaultScreen() }
//    ),

// TODO: Re-enable once we have Archive
//    Archive(
//        title = R.string.archive_screen_title,
//        content = {_,_-> ArchiveScreen() }
//    );

    companion object {
        // TODO: Re-enable once we have Archive & Vault
//         val all = listOf(Conversations, Archive, Vault)
        val all = listOf(Conversations)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class HomeTabsViewModels(
    val conversationListViewModel: ConversationListViewModel
)
