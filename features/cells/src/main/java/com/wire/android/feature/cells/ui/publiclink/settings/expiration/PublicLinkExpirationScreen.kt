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
package com.wire.android.feature.cells.ui.publiclink.settings.expiration

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.publiclink.PublicLinkErrorDialog
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSwitch
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.datetime.FutureSelectableDates
import com.wire.android.ui.common.datetime.TimePickerResult
import com.wire.android.ui.common.datetime.WireDatePickerDialog
import com.wire.android.ui.common.datetime.WireTimePickerDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.spacers.fillVerticalSpace
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalMaterial3Api::class)
@WireDestination(
    navArgsDelegate = PublicLinkExpirationScreenNavArgs::class
)
@Composable
internal fun PublicLinkExpirationScreen(
    resultNavigator: ResultBackNavigator<PublicLinkExpirationResult>,
    modifier: Modifier = Modifier,
    viewModel: PublicLinkExpirationScreenViewModel = hiltViewModel(),
) {

    val state by viewModel.state.collectAsState()

    var showDatePicker by remember { mutableStateOf<SelectedDate?>(null) }
    var showTimePicker by remember { mutableStateOf<SelectedTime?>(null) }
    var showError by remember { mutableStateOf<ExpirationError?>(null) }

    BackHandler {
        if (!state.showProgress) {
            resultNavigator.navigateBack(viewModel.getResult())
        }
    }

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = {
                    if (!state.showProgress) {
                        resultNavigator.navigateBack(viewModel.getResult())
                    }
                },
                title = stringResource(R.string.public_link_setting_expiration_title),
                navigationIconType = NavigationIconType.Back(),
                elevation = dimensions().spacing0x
            )
        }
    ) { innerPadding ->
        ExpirationScreenContent(
            state = state,
            modifier = Modifier.padding(innerPadding),
            onEnableClick = viewModel::onEnableClick,
            onTimeClick = viewModel::onTimeClick,
            onDateClick = viewModel::onDateClick,
            onSetClick = viewModel::setExpiration,
        )
    }

    showDatePicker?.let { selectedDate ->
        WireDatePickerDialog(
            title = stringResource(R.string.dialog_expiration_date_title),
            selectedDateMillis = selectedDate.date,
            selectableDates = FutureSelectableDates(),
            onDateSelected = { selectedDate ->
                showDatePicker = null
                selectedDate?.let {
                    viewModel.onDateSelected(selectedDate)
                }
            },
            onDismiss = {
                showDatePicker = null
            }
        )
    }

    showTimePicker?.let { selectedTime ->
        WireTimePickerDialog(
            title = stringResource(R.string.dialog_expiration_time_title),
            selectedTime = selectedTime.time,
            onTimeSelected = { result ->
                showTimePicker = null
                viewModel.onTimeSelected(result)
            },
            onDismiss = {
                showTimePicker = null
            },
        )
    }

    showError?.let { error ->
        PublicLinkErrorDialog(
            title = error.title?.let { stringResource(it) },
            message = error.message?.let { stringResource(it) },
            onResult = { tryAgain ->
                showError = null
                if (tryAgain) viewModel.retryError(error)
            }
        )
    }

    HandleActions(viewModel.actions) { action ->
        when (action) {
            is ShowDatePicker -> showDatePicker = SelectedDate(action.selectedDate)
            is ShowTimePicker -> showTimePicker = SelectedTime(action.selectedTime)
            is ShowError -> showError = action.error
            is CloseScreen -> resultNavigator.navigateBack(action.result)
        }
    }
}

@Composable
private fun ExpirationScreenContent(
    state: PublicLinkExpirationScreenViewState,
    modifier: Modifier = Modifier,
    onEnableClick: () -> Unit = {},
    onDateClick: () -> Unit = {},
    onTimeClick: () -> Unit = {},
    onSetClick: () -> Unit = {},
) {
    Column(modifier = modifier) {

        EnableExpirationSection(
            checked = state.isEnabled,
            onCheckClick = onEnableClick,
        )

        AnimatedVisibility(
            visible = state.isEnabled,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                modifier = Modifier.padding(dimensions().spacing16x)
            ) {

                VerticalSpace.x8()

                Text(
                    text = stringResource(R.string.label_expires).uppercase(),
                    color = if (state.isValidExpirationDate) colorsScheme().primary else colorsScheme().error,
                    style = typography().label01,
                )

                VerticalSpace.x8()

                DateTimeView(
                    date = state.date,
                    time = state.time,
                    onDateClick = onDateClick,
                    onTimeClick = onTimeClick,
                )

                if (!state.isValidExpirationDate) {
                    VerticalSpace.x8()
                    Text(
                        text = stringResource(R.string.invalid_expiration_date),
                        color = colorsScheme().error,
                        style = typography().label01,
                    )
                }

                fillVerticalSpace()

                WirePrimaryButton(
                    text = stringResource(R.string.public_link_expiration_set),
                    loading = state.showProgress,
                    state = if (state.isSetButtonEnabled && !state.showProgress) WireButtonState.Default else WireButtonState.Disabled,
                    onClick = onSetClick
                )
            }
        }
    }
}

@Composable
private fun EnableExpirationSection(
    checked: Boolean,
    onCheckClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorsScheme().surface)
            .clickable { onCheckClick() }
            .padding(dimensions().spacing16x)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f),
                text = stringResource(R.string.public_link_expiration_add),
                style = typography().body02
            )
            WireSwitch(
                checked = checked,
                onCheckedChange = { onCheckClick() },
            )
        }
        VerticalSpace.x16()
        Text(
            text = stringResource(R.string.public_link_expiration_description),
            style = typography().body01
        )
    }
}

data class PublicLinkExpirationScreenNavArgs(
    val linkUuid: String,
    val expiresAt: Long?,
)

@Parcelize
data class PublicLinkExpirationResult(
    val isExpirationSet: Boolean,
    val expiresAt: Long?,
) : Parcelable {

    @Suppress("FunctionNaming")
    companion object {
        fun Enabled(expiresAt: Long) = PublicLinkExpirationResult(
            isExpirationSet = true,
            expiresAt = expiresAt,
        )

        fun Disabled() = PublicLinkExpirationResult(
            isExpirationSet = false,
            expiresAt = null,
        )
    }
}

private data class SelectedDate(
    val date: Long?
)

private data class SelectedTime(
    val time: TimePickerResult?
)

@PreviewMultipleThemes
@Composable
private fun PreviewExpirationScreenContent() {
    WireTheme {
        ExpirationScreenContent(
            state = PublicLinkExpirationScreenViewState(isEnabled = true),
        )
    }
}
