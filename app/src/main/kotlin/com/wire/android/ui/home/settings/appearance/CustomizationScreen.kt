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

package com.wire.android.ui.home.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.selectableBackground
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.home.settings.SwitchState
import com.wire.android.ui.theme.ThemeData
import com.wire.android.ui.theme.ThemeOption
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.sectionWithElements
import com.wire.android.util.ui.PreviewMultipleThemes

@WireDestination
@Composable
fun CustomizationScreen(
    navigator: Navigator,
    viewModel: CustomizationViewModel = hiltViewModel()
) {
    val lazyListState: LazyListState = rememberLazyListState()
    CustomizationScreenContent(
        lazyListState = lazyListState,
        state = viewModel.state,
        onThemeOptionChanged = viewModel::selectThemeOption,
        onBackPressed = navigator::navigateBack,
        onEnterToSendClicked = viewModel::selectPressEnterToSendOption,
    )
}

@Composable
fun CustomizationScreenContent(
    state: CustomizationState,
    onThemeOptionChanged: (ThemeOption) -> Unit,
    onBackPressed: () -> Unit,
    onEnterToSendClicked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onBackPressed,
                elevation = 0.dp,
                title = stringResource(id = R.string.settings_customization_label)
            )
        }
    ) { internalPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxSize()
        ) {
            sectionWithElements(
                header = context.getString(R.string.settings_appearance_theme_label),
                items = buildList {
                    add(ThemeData(option = ThemeOption.SYSTEM, selectedOption = state.selectedThemeOption))
                    add(ThemeData(option = ThemeOption.LIGHT, selectedOption = state.selectedThemeOption))
                    add(ThemeData(option = ThemeOption.DARK, selectedOption = state.selectedThemeOption))
                },
                onItemClicked = onThemeOptionChanged
            )
            item {
                CustomizationOptionsContent(
                    enterToSendState = state.pressEnterToSentState,
                    enterToSendClicked = onEnterToSendClicked,
                )
            }
        }
    }
}

@Composable
fun CustomizationOptionsContent(
    enterToSendState: Boolean,
    enterToSendClicked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        SectionHeader(stringResource(R.string.customization_options_header_title))
        GroupConversationOptionsItem(
            title = stringResource(R.string.press_enter_to_send_title),
            switchState = SwitchState.Enabled(value = enterToSendState, onCheckedChange = enterToSendClicked),
            arrowType = ArrowType.NONE,
            subtitle = stringResource(id = R.string.press_enter_to_send_text)
        )
    }
}

private fun LazyListScope.sectionWithElements(
    header: String,
    items: List<ThemeData>,
    onItemClicked: (ThemeOption) -> Unit
) {
    sectionWithElements(
        header = header.uppercase(),
        items = items.associateBy { it.option }
    ) { themeItem ->
        ThemeOptionItem(
            themeOption = themeItem.option,
            selectedOption = themeItem.selectedOption,
            onItemClicked = onItemClicked
        )
    }
}

@Composable
fun ThemeOptionItem(
    themeOption: ThemeOption,
    selectedOption: ThemeOption,
    onItemClicked: (ThemeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = themeOption == selectedOption
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = dimensions().spacing1x)
            .selectableBackground(isSelected, onClick = { onItemClicked(themeOption) })
            .background(color = MaterialTheme.wireColorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = { onItemClicked(themeOption) })
        Text(
            text = when (themeOption) {
                ThemeOption.LIGHT -> buildAnnotatedString { append(stringResource(R.string.settings_appearance_theme_option_light)) }
                ThemeOption.DARK -> buildAnnotatedString { append(stringResource(R.string.settings_appearance_theme_option_dark)) }
                ThemeOption.SYSTEM -> buildAnnotatedString {
                    append(stringResource(R.string.settings_appearance_theme_option_system))
                    append(" ")
                    withStyle(
                        style = SpanStyle(color = MaterialTheme.wireColorScheme.secondaryText)
                    ) {
                        append(stringResource(R.string.settings_appearance_theme_option_default_hint))
                    }
                }
            },
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.onBackground,
            modifier = Modifier.padding(vertical = dimensions().spacing16x)
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSettingsScreen() {
    CustomizationScreenContent(
        CustomizationState(),
        {},
        {},
        {},
    )
}
