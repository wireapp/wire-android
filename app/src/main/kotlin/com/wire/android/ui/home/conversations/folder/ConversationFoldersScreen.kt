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
package com.wire.android.ui.home.conversations.folder

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.button.WireButton
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography

@RootNavGraph
@WireDestination(
    navArgsDelegate = ConversationFoldersNavArgs::class,
    style = PopUpNavigationAnimation::class
)
@Composable
fun ConversationFoldersScreen(
    navigator: Navigator,
) {
    Content(
        onNavigationPressed = { navigator.navigateBack() },
    )
}

@Composable
private fun Content(
    onNavigationPressed: () -> Unit = {},
) {
    val resources = LocalContext.current.resources
    val context = LocalContext.current
    WireScaffold(
        modifier = Modifier
            .background(color = colorsScheme().surfaceContainerLow),

        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.label_move_to_folder),
                navigationIconType = NavigationIconType.Close(),
                onNavigationPressed = onNavigationPressed,
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(dimensions().spacing16x)) {
                WireSecondaryButton(
                    state = WireButtonState.Default,
                    text = stringResource(id = R.string.label_new_folder),
                    onClick = {
                            Toast.makeText(
                                context,
                                "Not implemented yet",
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                )
                VerticalSpace.x8()
                WireButton(
                    state = WireButtonState.Disabled,
                    text = stringResource(id = R.string.label_done),
                    onClick = { }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
          if(true) { // TODO KBX implement fetching folders
              Text(
                  stringResource(R.string.folder_create_description),
                  modifier = Modifier.align(Alignment.Center),
                  style = typography().body01,
                  color = colorsScheme().secondaryText)

          } else {
              LazyColumn(Modifier.fillMaxHeight()) {

              }
          }
        }
    }
}
