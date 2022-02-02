package com.wire.android.ui.home.conversationslist

import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.main.conversationlist.navigation.ConversationsNavigationItem


@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ConversationRouter(conversationListViewModel: ConversationListViewModel = hiltViewModel()) {
    val uiState by conversationListViewModel.listState.collectAsState()
    val navController = rememberNavController()

    val state = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )

    val scope = rememberCoroutineScope()

    fun navigateToConversation(id: String) {
        conversationListViewModel.openConversation(id)
    }

    Scaffold(
        floatingActionButton = { FloatingActionButton(stringResource(R.string.label_new), {}) },
        bottomBar = { WireBottomNavigationBar(ConversationNavigationItems(uiState), navController) }
    ) {
        with(uiState) {
            NavHost(navController, startDestination = ConversationsNavigationItem.All.route) {
                composable(
                    route = ConversationsNavigationItem.All.route,
                    content = {
                        AllConversationScreen(
                            newActivities = newActivities,
                            conversations = conversations,
                            onOpenConversationClick = ::navigateToConversation
                        )
                    })
                composable(
                    route = ConversationsNavigationItem.Calls.route,
                    content = {
                        CallScreen(
                            missedCalls = missedCalls,
                            callHistory = callHistory,
                            onCallItemClick = ::navigateToConversation
                        )
                    })
                composable(
                    route = ConversationsNavigationItem.Mentions.route,
                    content = {
                        MentionScreen(
                            unreadMentions = unreadMentions,
                            allMentions = allMentions,
                            onMentionItemClick = ::navigateToConversation
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ConversationNavigationItems(
    uiListState: ConversationListState
): List<WireBottomNavigationItemData> {
    return ConversationsNavigationItem.values().map { conversationsNavigationItem ->
        when (conversationsNavigationItem) {
            ConversationsNavigationItem.All -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.newActivityCount)
            ConversationsNavigationItem.Calls -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.missedCallsCount)
            ConversationsNavigationItem.Mentions -> conversationsNavigationItem.toBottomNavigationItemData(uiListState.unreadMentionsCount)
        }
    }
}


