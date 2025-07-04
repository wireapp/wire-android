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
package com.wire.android.ui.home.settings.about.licenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.util.htmlReadyLicenseContent
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar

@WireDestination
@Composable
fun LicensesScreen(
    navigator: Navigator,
    viewModel: LicensesViewModel = hiltViewModel()
) {
    WireScaffold(topBar = {
        WireCenterAlignedTopAppBar(
            onNavigationPressed = navigator::navigateBack,
            elevation = 0.dp,
            title = stringResource(id = R.string.settings_licenses_settings_label)
        )
    }) { internalPadding ->
        LicensesContent(
            internalPadding = internalPadding,
            libs = viewModel.state.libraryList
        )
    }
}

@Composable
fun LicensesContent(
    internalPadding: PaddingValues,
    libs: List<Library>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(internalPadding)
    ) {
        val openDialog = remember { mutableStateOf<Library?>(null) }
        WireLibraries(
            modifier = Modifier.fillMaxSize(),
            libraries = libs,
            onLibraryClick = { library ->
                val license = library.licenses.firstOrNull()

                if (!license?.htmlReadyLicenseContent.isNullOrBlank()) {
                    openDialog.value = library
                }
            },
        )

        val library = openDialog.value
        if (library != null) {
            WireLicenseDialog(library = library) {
                openDialog.value = null
            }
        }
    }
}
