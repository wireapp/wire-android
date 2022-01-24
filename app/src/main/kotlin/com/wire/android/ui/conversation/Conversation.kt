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
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.conversation.navigation.ConversationsNavigationGraph
import com.wire.android.ui.conversation.navigation.ConversationsNavigationItem

@Composable
fun Conversation() {
    val navController = rememberNavController()
    val items = ConversationsNavigationItem.values().map { it.intoBottomNavigationItemData(12) }

    Scaffold(
        floatingActionButton = { ConversationListFloatingActionButton() },
        bottomBar = { WireBottomNavigationBar(items, navController) }
    ) {
        ConversationsNavigationGraph(navController = navController)
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
