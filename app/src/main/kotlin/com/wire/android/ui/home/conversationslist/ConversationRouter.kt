package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.CircularProgressIndicator
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.main.conversationlist.navigation.ConversationsNavigationItem
import kotlinx.coroutines.launch


class ModalSheetContentState {
    var title: String by mutableStateOf("")

    var avatar: ModalSheetAvatar by mutableStateOf(ModalSheetAvatar.None)
}

sealed class ModalSheetAvatar {
    data class UserAvatar(val avatarUrl: String) : ModalSheetAvatar()
    data class GroupAvatar(val groupColor: Long) : ModalSheetAvatar()
    object None : ModalSheetAvatar()
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ConversationRouter(conversationListViewModel: ConversationListViewModel = hiltViewModel()) {
    val uiState by conversationListViewModel.listState.collectAsState()
    val navController = rememberNavController()
    val state = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )

    val modalSheetContentState by remember {
        mutableStateOf(ModalSheetContentState())
    }

    val scope = rememberCoroutineScope()

    fun navigateToConversation(id: String) {
        conversationListViewModel.openConversation(id)
    }

    fun showModalSheet(conversationType: ConversationType) {
        when (conversationType) {
            is ConversationType.GroupConversation -> {
                with(conversationType) {
                    modalSheetContentState.avatar = ModalSheetAvatar.GroupAvatar(groupColorValue)
                    modalSheetContentState.title = groupName
                }
            }
            is ConversationType.PrivateConversation -> {
                with(conversationType) {
                    modalSheetContentState.avatar = ModalSheetAvatar.UserAvatar(userInfo.avatarUrl)
                    modalSheetContentState.title = conversationInfo.name
                }
            }
        }
        scope.launch { state.show() }
    }

    ModalBottomSheetLayout(
        sheetState = state,
        sheetContent = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (val avatar = modalSheetContentState.avatar) {
                        is ModalSheetAvatar.GroupAvatar -> GroupConversationAvatar(colorValue = avatar.groupColor)
                        is ModalSheetAvatar.UserAvatar -> UserProfileAvatar()
                        ModalSheetAvatar.None -> CircularProgressIndicator(progressColor = Color.Blue)
                    }
                    Text(modalSheetContentState.title)
                }
            }
        }
    ) {
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
                                onOpenConversationClick = ::navigateToConversation,
                                onEditConversationItem = ::showModalSheet
                            )
                        })
                    composable(
                        route = ConversationsNavigationItem.Calls.route,
                        content = {
                            CallScreen(
                                missedCalls = missedCalls,
                                callHistory = callHistory,
                                onCallItemClick = ::navigateToConversation,
                                onEditConversationItem = ::showModalSheet
                            )
                        })
                    composable(
                        route = ConversationsNavigationItem.Mentions.route,
                        content = {
                            MentionScreen(
                                unreadMentions = unreadMentions,
                                allMentions = allMentions,
                                onMentionItemClick = ::navigateToConversation,
                                onEditConversationItem = ::showModalSheet
                            )
                        }
                    )
                }
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


