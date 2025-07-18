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
package com.wire.android.ui.home.settings.about.dependencies

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@Composable
@WireDestination
fun DependenciesScreen(
    navigator: Navigator,
    viewModel: DependenciesViewModel = hiltViewModel()
) {
    WireScaffold(topBar = {
        WireCenterAlignedTopAppBar(
            onNavigationPressed = navigator::navigateBack,
            elevation = 0.dp,
            title = stringResource(id = R.string.settings_dependencies_label)
        )
    }) { internalPadding ->
        DependenciesContent(
            internalPadding = internalPadding,
            dependencies = viewModel.state.dependencies
        )
    }
}

@Composable
private fun DependenciesContent(
    internalPadding: PaddingValues,
    dependencies: ImmutableMap<String, String?>,
) {
    val lazyListState = rememberLazyListState()
    LazyColumn(
        Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = internalPadding
    ) {
        dependencies.entries.forEach {
            item {
                DependenciesItem(dependencyName = it.key, dependencyVersion = it.value.orEmpty())
            }
        }
    }
}

@Composable
private fun DependenciesItem(
    dependencyName: String,
    dependencyVersion: String
) {
    RowItemTemplate(
        modifier = Modifier.wrapContentWidth(),
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = "${dependencyName.uppercase()}: $dependencyVersion",
            )
        }
    )
}

@Composable
@PreviewMultipleThemes
fun DependenciesContentPreview() {
    DependenciesContent(
        internalPadding = PaddingValues(dimensions().spacing8x),
        dependencies = persistentMapOf("avs" to "4.10.1", "cc" to "0.0.1")
    )
}
