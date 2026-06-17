/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.userprofile.self.status

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.WireDropDown
import com.wire.android.ui.common.avatar.UserStatusIndicator
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.emoji.EmojiPickerBottomSheet
import com.wire.android.ui.home.settings.selfUserStatusViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.self.dialog.ChangeStatusDialogContent
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.android.ui.common.R as UICommonR

@WireRootDestination(
    style = PopUpNavigationAnimation::class,
)
@Composable
@SuppressLint("ComposeModifierMissing")
fun SelfUserStatusScreen(
    navigator: Navigator,
    viewModel: SelfUserStatusViewModel = selfUserStatusViewModel(),
) {
    SelfUserStatusContent(
        state = viewModel.state,
        onBackClick = navigator::navigateBack,
        onAvailabilityStatusClicked = viewModel::changeAvailabilityStatusClick,
        onAvailabilityStatusChange = viewModel::changeAvailabilityStatus,
        onDismissStatusDialog = viewModel::dismissStatusDialog,
        onNotShowRationaleAgainChange = viewModel::dialogCheckBoxStateChanged,
        onEmojiSelected = viewModel::onEmojiSelected,
        onUpdateCustomStatus = viewModel::updateCustomStatus,
    )
}

@Composable
private fun SelfUserStatusContent(
    state: SelfUserStatusState,
    onBackClick: () -> Unit,
    onAvailabilityStatusClicked: (UserAvailabilityStatus) -> Unit,
    onAvailabilityStatusChange: (UserAvailabilityStatus) -> Unit,
    onDismissStatusDialog: () -> Unit,
    onNotShowRationaleAgainChange: (Boolean) -> Unit,
    onEmojiSelected: (String) -> Unit,
    onUpdateCustomStatus: (String) -> Unit,
) {
    val textState = rememberTextFieldState(state.message)
    val emojiPickerState = rememberWireModalSheetState<Unit>(skipPartiallyExpanded = false)

    LaunchedEffect(state.message) {
        if (textState.text.toString() != state.message) {
            textState.setTextAndPlaceCursorAtEnd(state.message)
        }
    }

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onBackClick,
                title = stringResource(R.string.user_profile_status_title),
                navigationIconType = NavigationIconType.Back(),
                elevation = 0.dp,
            )
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            AvailabilitySection(
                userStatus = state.availabilityStatus,
                onStatusClicked = onAvailabilityStatusClicked,
            )
            SelectedAvailabilityDescription(state.availabilityStatus)
            VerticalSpace.x16()
            if (state.isTeamMember) {
                CustomStatusSection(
                    emoji = state.emoji,
                    textState = textState,
                    messageLength = textState.text.length,
                    onEmojiClick = { emojiPickerState.show() },
                    onUpdateCustomStatus = { onUpdateCustomStatus(textState.text.toString()) },
                    isSaving = state.isSaving,
                )
            }
        }

        ChangeStatusDialogContent(
            data = state.statusDialogData,
            dismiss = onDismissStatusDialog,
            onStatusChange = onAvailabilityStatusChange,
            onNotShowRationaleAgainChange = onNotShowRationaleAgainChange
        )

        EmojiPickerBottomSheet(
            sheetState = emojiPickerState,
            onEmojiSelected = { emoji, _ ->
                emojiPickerState.hide()
                onEmojiSelected(emoji)
            }
        )
    }
}

@SuppressLint("ComposeModifierMissing")
@Composable
fun SelectedAvailabilityDescription(status: UserAvailabilityStatus) {
    val description = when (status) {
        UserAvailabilityStatus.NONE -> R.string.user_profile_change_status_dialog_none_text
        UserAvailabilityStatus.AVAILABLE -> R.string.user_profile_change_status_dialog_available_text
        UserAvailabilityStatus.BUSY -> R.string.user_profile_change_status_dialog_busy_text
        UserAvailabilityStatus.AWAY -> R.string.user_profile_change_status_dialog_away_text
    }
    Text(
        text = stringResource(description),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
        style = MaterialTheme.wireTypography.body01,
        color = MaterialTheme.wireColorScheme.secondaryText,
    )
}

@Composable
private fun AvailabilitySection(
    userStatus: UserAvailabilityStatus,
    onStatusClicked: (UserAvailabilityStatus) -> Unit,
) {
    val items = listOf(
        UserAvailabilityStatus.AVAILABLE,
        UserAvailabilityStatus.BUSY,
        UserAvailabilityStatus.AWAY,
        UserAvailabilityStatus.NONE
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.user_profile_status_availability))
        WireDropDown(
            items = items.map {
                when (it) {
                    UserAvailabilityStatus.AVAILABLE -> stringResource(UICommonR.string.user_profile_status_available)
                    UserAvailabilityStatus.BUSY -> stringResource(UICommonR.string.user_profile_status_busy)
                    UserAvailabilityStatus.AWAY -> stringResource(UICommonR.string.user_profile_status_away)
                    UserAvailabilityStatus.NONE -> stringResource(UICommonR.string.user_profile_status_none)
                }
            },
            defaultItemIndex = items.indexOf(userStatus),
            label = null,
            modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x),
            autoUpdateSelection = false,
            showDefaultTextIndicator = false,
            leadingCompose = { index -> UserStatusIndicator(items[index]) },
            onChangeClickDescription = stringResource(R.string.content_description_self_profile_change_status)
        ) { selectedIndex ->
            onStatusClicked(items[selectedIndex])
        }
    }
}

@Composable
private fun CustomStatusSection(
    emoji: String,
    textState: TextFieldState,
    messageLength: Int,
    onEmojiClick: () -> Unit,
    onUpdateCustomStatus: () -> Unit,
    isSaving: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.user_profile_custom_status_header))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions().spacing16x),
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WireSecondaryButton(
                text = emoji,
                onClick = onEmojiClick,
                fillMaxWidth = false,
                minSize = MaterialTheme.wireDimensions.buttonSmallMinSize,
                minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
            )
            WireTextField(
                textState = textState,
                modifier = Modifier.weight(1f),
                placeholderText = stringResource(R.string.user_profile_custom_status_placeholder),
                leadingIcon = null,
                state = WireTextFieldState.Default,
                inputTransformation = InputTransformation.maxLength(MAX_STATUS_TEXT_LENGTH),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
            )
        }
        Text(
            text = stringResource(R.string.user_profile_custom_status_counter, messageLength, MAX_STATUS_TEXT_LENGTH),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
            style = MaterialTheme.wireTypography.body02,
            color = MaterialTheme.wireColorScheme.secondaryText,
        )
        WirePrimaryButton(
            modifier = Modifier.padding(horizontal = dimensions().spacing16x),
            text = stringResource(R.string.user_profile_update_status),
            loading = isSaving,
            onClick = onUpdateCustomStatus,
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun SelfUserStatusContentPreview() {
    WireTheme {
        SelfUserStatusContent(
            state = SelfUserStatusState(
                availabilityStatus = UserAvailabilityStatus.AVAILABLE,
                emoji = "👋",
                message = "Working from home",
                isTeamMember = true,
            ),
            onBackClick = {},
            onAvailabilityStatusClicked = {},
            onAvailabilityStatusChange = {},
            onDismissStatusDialog = {},
            onNotShowRationaleAgainChange = {},
            onEmojiSelected = {},
            onUpdateCustomStatus = {},
        )
    }
}
