/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.cell

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.navigation.WireDestination
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi

@HomeNavGraph
@WireDestination
@Composable
fun WireCellScreen(
    viewModel: CellViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()

    CellScreenContent(
        state = state,
        onFileClick = {},
    )

    LaunchedEffect(Unit) {
        viewModel.loadFiles()
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiMessage.collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun CellScreenContent(
    state: CellViewState,
    onFileClick: (AttachmentDraftUi) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(
                items = state.files,
                key = {
                    when (it) {
                        is CellListHeader -> it.title
                        is CellNodeItem -> it.node.uuid
                    }
                },
            ) { file ->
                when (file) {
                    is CellListHeader -> Header(title = file.title)
                    is CellNodeItem -> CellListItem(
                        modifier = Modifier
                            .animateItem()
                            .clickable { onFileClick(file.node) },
                        file = file.node,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .padding(top = 16.dp),
    )
}
