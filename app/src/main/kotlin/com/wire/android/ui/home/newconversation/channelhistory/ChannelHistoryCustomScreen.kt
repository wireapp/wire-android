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
package com.wire.android.ui.home.newconversation.channelhistory

import com.wire.android.navigation.annotation.app.WireNewConversationDestination
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.R
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.common.WireDropDown
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.DefaultCode
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.maxLengthDigits
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@WireNewConversationDestination(
    navArgs = ChannelHistoryCustomArgs::class,
    style = SlideNavigationAnimation::class,
)
@Composable
fun ChannelHistoryCustomScreen(
    navArgs: ChannelHistoryCustomArgs,
    resultNavigator: ResultBackNavigator<ChannelHistoryCustomNavBackArgs>,
    modifier: Modifier = Modifier,
) {
    val specificCurrentType = navArgs.currentType as? ChannelHistoryType.On.Specific
    val amountState = rememberTextFieldState(specificCurrentType?.amount?.toString().orEmpty())
    var timeState by rememberSaveable {
        mutableStateOf(specificCurrentType?.type ?: ChannelHistoryType.On.Specific.AmountType.Days)
    }

    fun navigateBack() {
        amountState.text.toString().toIntOrNull()?.takeIf { it > 0 }?.let {
            resultNavigator.setResult(ChannelHistoryCustomNavBackArgs(ChannelHistoryType.On.Specific(it, timeState)))
        }
        resultNavigator.navigateBack()
    }

    BackHandler(enabled = true, onBack = ::navigateBack)

    ChannelHistoryCustomScreenContent(
        amountState = amountState,
        typeState = timeState,
        onTypeSelected = { timeState = it },
        onBackPressed = ::navigateBack,
        modifier = modifier,
    )
}

@Composable
fun ChannelHistoryCustomScreenContent(
    amountState: TextFieldState,
    typeState: ChannelHistoryType.On.Specific.AmountType,
    onTypeSelected: (ChannelHistoryType.On.Specific.AmountType) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onBackPressed,
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.channel_history_custom),
                titleContentDescription = stringResource(id = R.string.content_description_new_conversation_history_custom_heading),
                navigationIconType = NavigationIconType.Back(R.string.content_description_new_conversation_history_back_btn)
            )
        }
    ) { internalPadding ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxWidth()
                .padding(horizontal = dimensions().spacing16x)
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current
            WireTextField(
                textState = amountState,
                labelText = stringResource(R.string.channel_history_custom_amount_label),
                keyboardOptions = KeyboardOptions.Companion.DefaultCode,
                inputTransformation = InputTransformation.maxLengthDigits(MAX_AMOUNT_LENGTH),
                onKeyboardAction = { keyboardController?.hide() },
                modifier = Modifier.weight(weight = 1f, fill = true),
            )
            val items = ChannelHistoryType.On.Specific.AmountType.entries
            val itemsNames = items.map { pluralStringResource(it.nameResId, amountState.text.toString().toIntOrNull() ?: 0) }
            WireDropDown(
                items = itemsNames,
                defaultItemIndex = 0,
                selectedItemIndex = items.indexOf(typeState),
                label = stringResource(R.string.channel_history_custom_time_label),
                autoUpdateSelection = true,
                showDefaultTextIndicator = false,
                onChangeClickDescription = stringResource(R.string.content_description_new_conversation_history_custom_change_time),
                onSelected = { onTypeSelected(items[it]) },
                modifier = Modifier.weight(weight = 3f, fill = true)
            )
        }
    }
}

private const val MAX_AMOUNT_LENGTH = 2

@PreviewMultipleThemes
@Composable
fun PreviewChannelHistoryCustomScreen() = WireTheme {
    ChannelHistoryCustomScreenContent(
        amountState = rememberTextFieldState("1"),
        typeState = ChannelHistoryType.On.Specific.AmountType.Days,
        onTypeSelected = {},
        onBackPressed = {},
    )
}
