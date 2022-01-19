package com.wire.android.ui.main.convesations

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.common.WireBottomNavigationBar

@Composable
fun ConversationsScreen() {
    val navController = rememberNavController()
    val items = ConversationsWireBottomNavigationItemData.values()
        .map { it.intoBottomNavigationItemData(12) }

    Scaffold(
        bottomBar = {
            WireBottomNavigationBar(navController = navController, items = items)
        }
    ) {
        ConversationsNavigationGraph(navController = navController)
    }
}

@Preview(showBackground = false)
@Composable
fun ConversationsScreenPreview() {
    ConversationsScreen()
}
