package com.wire.android.ui.conversation

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.conversation.navigation.ConversationsNavigationItem


@Composable
fun Conversation(viewModel: ConversationViewModel = ConversationViewModel()) {
    val uiState by viewModel.state.collectAsState()
    val navController = rememberNavController()

    Scaffold(
        floatingActionButton = { ConversationListFloatingActionButton() },
        bottomBar = { WireBottomNavigationBar(ConversationNavigationItems(uiState), navController) }
    ) {
        with(uiState) {
            NavHost(navController, startDestination = "conversations_all") {
                composable(route = "conversations_all", content = { AllConversationScreen(newActivities, conversations) })
                composable(route = "conversations_calls", content = { CallScreen(missedCalls, callHistory) })
                composable(route = "conversations_mentions", content = { MentionScreen(unreadMentions, allMentions) })
            }
        }
    }
}

@Composable
private fun ConversationNavigationItems(
    uiState: ConversationState
): List<WireBottomNavigationItemData> {
    return ConversationsNavigationItem.values().map {
        when (it) {
            ConversationsNavigationItem.All -> it.toBottomNavigationItemData(uiState.newActivityCount)
            ConversationsNavigationItem.Calls -> it.toBottomNavigationItemData(uiState.missedCallsCount)
            ConversationsNavigationItem.Mentions -> it.toBottomNavigationItemData(uiState.unreadMentionsCount)
        }
    }
}

@Composable
private fun ConversationListFloatingActionButton() {
    ExtendedFloatingActionButton(
        shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 30)),
        icon = { Icon(Icons.Filled.Add, "") },
        text = { Text(text = stringResource(R.string.label_new)) },
        onClick = { })
}
