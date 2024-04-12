/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.calling

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.calling.incoming.IncomingCallScreen
import com.wire.android.ui.calling.initiating.InitiatingCallScreen
import com.wire.android.ui.calling.ongoing.OngoingCallScreen
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun CallScreen(
    conversationId: ConversationId,
    startDestination: CallScreenType,
    navController: NavHostController = rememberNavController()
) {

    Scaffold(
        topBar = { Column(Modifier.size(0.dp)) { } },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "${startDestination.name}/$conversationId",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(
                route = "${CallScreenType.Incoming.name}/{conversationId}",
            ) {
                IncomingCallScreen(
                    conversationId = conversationId,
                    onCallAccepted = {
                        navController.navigate("${CallScreenType.Ongoing.name}/$conversationId")
                    }
                )
            }
            composable(route = "${CallScreenType.Ongoing.name}/{conversationId}") {
                OngoingCallScreen(conversationId)
            }
            composable(route = "${CallScreenType.Initiating.name}/{conversationId}") {
                InitiatingCallScreen(conversationId) {
                    navController.navigate("${CallScreenType.Ongoing.name}/$conversationId")
                }
            }
        }
    }
}

enum class CallScreenType {
    Incoming,
    Ongoing,
    Initiating
}
