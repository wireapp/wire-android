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
package com.wire.android.feature.meetings.ui.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import com.ramcosta.composedestinations.generated.meetings.destinations.NewMeetingParticipantsScreenDestination
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModel.Companion.MEETING_NAME_MAX_COUNT
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.model.Contact
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.meetings.WireNewMeetingDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDropDown
import com.wire.android.ui.common.animation.ShakeAnimation
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.datetime.FutureSelectableDates
import com.wire.android.ui.common.datetime.WireDatePickerDialog
import com.wire.android.ui.common.datetime.WireTimePickerDialog
import com.wire.android.ui.common.datetime.asTimePickerResult
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.DefaultEmailDone
import com.wire.android.ui.common.textfield.DefaultText
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.maxLengthWithCallback
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CurrentTimeProvider
import com.wire.android.util.DateAndTimeParsers
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.user.ConnectionState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import com.wire.android.ui.common.R as commonR

@WireNewMeetingDestination(
    start = true,
    navArgs = NewMeetingNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun NewMeetingScreen(
    navigator: WireNavigator,
    navArgs: NewMeetingNavArgs,
    newMeetingViewModel: NewMeetingViewModel,
) {
    NewMeetingContent(
        type = navArgs.type,
        onBackPressed = navigator::navigateBack,
        state = newMeetingViewModel.state,
        titleState = newMeetingViewModel.titleTextState,
        onParticipantsClicked = {
            navigator.navigate(NavigationCommand(NewMeetingParticipantsScreenDestination))
        },
        onCreateClicked = newMeetingViewModel::createMeeting,
        onStartTimeChanged = newMeetingViewModel::updateStartTime,
        onEndTimeChanged = newMeetingViewModel::updateEndTime,
        onRepeatingIntervalChanged = newMeetingViewModel::updateRepeatingInterval,
    )

    HandleActions(newMeetingViewModel.actions) { action ->
        when (action) {
            is NewMeetingViewActions.Success -> navigator.navigateBack()
        }
    }
}

@Composable
fun NewMeetingContent(
    state: NewMeetingState,
    titleState: TextFieldState,
    type: NewMeetingType,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onParticipantsClicked: () -> Unit = {},
    onCreateClicked: () -> Unit = {},
    onStartTimeChanged: (startTime: Instant) -> Unit = {},
    onEndTimeChanged: (endTime: Instant) -> Unit = {},
    onRepeatingIntervalChanged: (interval: MeetingItem.RepeatingInterval) -> Unit = {},
) {
    val scrollState = rememberScrollState()
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                title = stringResource(type.title),
                onNavigationPressed = onBackPressed,
                navigationIconType = NavigationIconType.Back(
                    contentDescription = R.string.content_description_new_meeting_back_icon
                ),
            )
        },
        content = { internalPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(internalPadding)
                    .verticalScroll(scrollState)
                    .padding(
                        vertical = dimensions().spacing24x,
                        horizontal = dimensions().spacing16x,
                    )
            ) {
                TitleInput(
                    titleState = titleState,
                    titleError = state.titleError,
                )
                if (type == NewMeetingType.Schedule) {
                    VerticalSpace.x24()
                    TimeInput(
                        time = state.startTime,
                        timeError = state.startTimeError,
                        onTimeChanged = onStartTimeChanged,
                        label = stringResource(R.string.new_meeting_starts_input_label),
                        datePlaceholder = stringResource(R.string.new_meeting_start_date_input_placeholder),
                        timePlaceholder = stringResource(R.string.new_meeting_start_time_input_placeholder),
                    )
                    VerticalSpace.x8()
                    TimeInput(
                        time = state.endTime,
                        timeError = state.endTimeError,
                        onTimeChanged = onEndTimeChanged,
                        label = stringResource(R.string.new_meeting_ends_input_label),
                        datePlaceholder = stringResource(R.string.new_meeting_end_date_input_placeholder),
                        timePlaceholder = stringResource(R.string.new_meeting_end_time_input_placeholder),
                    )
                    VerticalSpace.x8()
                    RepeatingIntervalDropDown(
                        repeatingInterval = state.repeatingInterval,
                        onRepeatingIntervalChanged = onRepeatingIntervalChanged,
                    )
                }
                VerticalSpace.x24()
                ParticipantsInput(
                    participants = state.confirmedContacts,
                    onClick = onParticipantsClicked,
                )
            }
        },
        bottomBar = {
            Surface(
                shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
                color = MaterialTheme.wireColorScheme.background,
                modifier = Modifier.fillMaxWidth(),
            ) {
                WirePrimaryButton(
                    text = stringResource(type.action),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(type.icon),
                            contentDescription = null, // no separate content description as the text already describes the action
                            modifier = Modifier.padding(dimensions().spacing4x),
                        )
                    },
                    state = if (state.continueButtonEnabled) WireButtonState.Default else WireButtonState.Disabled,
                    onClick = onCreateClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensions().spacing16x),
                )
            }
        }
    )
}

@Composable
private fun TitleInput(
    titleState: TextFieldState,
    titleError: NewMeetingState.TitleError?,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    ShakeAnimation { animate ->
        WireTextField(
            textState = titleState,
            state = when (titleError) {
                is NewMeetingState.TitleError.TitleEmptyError ->
                    WireTextFieldState.Error(stringResource(R.string.new_meeting_title_name_error_empty))
                is NewMeetingState.TitleError.TitleExceedsLimitError ->
                    WireTextFieldState.Error(stringResource(R.string.new_meeting_title_name_error_exceeded_limit))
                else -> WireTextFieldState.Default
            },
            placeholderText = stringResource(R.string.new_meeting_title_input_placeholder),
            labelText = stringResource(R.string.new_meeting_title_input_label).uppercase(),
            semanticDescription = stringResource(R.string.new_meeting_title_input_placeholder),
            keyboardOptions = KeyboardOptions.DefaultText,
            onKeyboardAction = { keyboardController?.hide() },
            testTag = "titleInput",
            inputTransformation = InputTransformation.maxLengthWithCallback(MEETING_NAME_MAX_COUNT, animate),
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .width(dimensions().spacing64x)
                        .height(dimensions().spacing40x),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    AnimatedVisibility(
                        visible = titleState.text.isNotBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            modifier = Modifier.padding(start = dimensions().spacing12x),
                            onClick = titleState::clearText,
                        ) {
                            Icon(
                                painter = painterResource(id = commonR.drawable.ic_clear_search),
                                contentDescription = stringResource(commonR.string.content_description_clear_content)
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun ParticipantsInput(
    participants: ImmutableSet<Contact>,
    onClick: () -> Unit,
) {
    val resources = LocalResources.current
    val textStyle = typography().body01
    val textColor = colorsScheme().onSurface
    val suffixColor = colorsScheme().secondaryText
    val textMeasurer = rememberTextMeasurer()
    val textFieldState = rememberTextFieldState(participants.joinToString(", ") { it.name })
    var innerTextWidthPx by remember { mutableIntStateOf(0) }
    val truncationTransformation = remember(innerTextWidthPx) {
        TextListTruncationTransformation(
            availableWidthPx = innerTextWidthPx,
            textMeasurer = textMeasurer,
            textStyle = textStyle,
            textColor = textColor,
            suffixColor = suffixColor,
            provideSuffixText = { count ->
                resources.getQuantityString(R.plurals.new_meeting_participants_input_more_suffix, count, count)
            },
        )
    }

    LaunchedEffect(participants) {
        textFieldState.setTextAndPlaceCursorAtEnd(participants.joinToString(", ") { it.name })
    }

    val semanticDescription = stringResource(R.string.new_meeting_participants_input_placeholder)
    WireTextField(
        textState = textFieldState,
        placeholderText = stringResource(R.string.new_meeting_participants_input_placeholder),
        labelText = stringResource(R.string.new_meeting_participants_input_label).uppercase(),
        keyboardOptions = KeyboardOptions.DefaultEmailDone,
        state = WireTextFieldState.Default,
        readOnly = true,
        onInputSizeChanged = { innerTextWidthPx = it.width },
        outputTransformation = truncationTransformation,
        onTap = onClick,
        trailingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_expand),
                contentDescription = null,
                tint = colorsScheme().onSurfaceVariant,
                modifier = Modifier
                    .padding(start = dimensions().spacing4x, end = dimensions().spacing16x)
                    .size(dimensions().spacing16x)
            )
        },
        inputModifier = Modifier.clearAndSetSemantics {
            contentDescription = semanticDescription
            role = Role.Button
        }
    )
}

@Composable
private fun TimeInput(
    time: Instant,
    timeError: NewMeetingState.TimeError?,
    onTimeChanged: (Instant) -> Unit,
    label: String,
    datePlaceholder: String,
    timePlaceholder: String,
) {
    val dateTextFieldState = rememberTextFieldState(DateAndTimeParsers.meetingDate(time))
    val timeTextFieldState = rememberTextFieldState(DateAndTimeParsers.meetingTime(time))
    val datePickerDialogState = rememberVisibilityState<Unit>()
    val timePickerDialogState = rememberVisibilityState<Unit>()

    LaunchedEffect(time) {
        dateTextFieldState.setTextAndPlaceCursorAtEnd(DateAndTimeParsers.meetingDate(time))
        timeTextFieldState.setTextAndPlaceCursorAtEnd(DateAndTimeParsers.meetingTime(time))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(-dimensions().spacing1x) // Pulls elements together by 1dp
        ) {
            WireTextField(
                textState = dateTextFieldState,
                placeholderText = datePlaceholder,
                labelText = label.uppercase(),
                keyboardOptions = KeyboardOptions.DefaultEmailDone,
                state = if (timeError != null) WireTextFieldState.Error() else WireTextFieldState.Default,
                readOnly = true,
                shape = RoundedCornerShape(
                    topStart = dimensions().textFieldCornerSize,
                    bottomStart = dimensions().textFieldCornerSize,
                    topEnd = dimensions().spacing0x,
                    bottomEnd = dimensions().spacing0x,
                ),
                onTap = {
                    datePickerDialogState.show(Unit)
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(commonR.drawable.ic_calendar),
                        contentDescription = null,
                        tint = colorsScheme().onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = dimensions().spacing4x, end = dimensions().spacing16x)
                            .size(dimensions().spacing16x)
                    )
                },
                modifier = Modifier.weight(2f),
                inputModifier = Modifier.clearAndSetSemantics {
                    contentDescription = datePlaceholder
                    role = Role.Button
                }
            )
            WireTextField(
                textState = timeTextFieldState,
                labelText = String.EMPTY, // Time input doesn't have a label as the date input's label already describes the field
                keyboardOptions = KeyboardOptions.DefaultEmailDone,
                state = if (timeError != null) WireTextFieldState.Error() else WireTextFieldState.Default,
                readOnly = true,
                shape = RoundedCornerShape(
                    topStart = dimensions().spacing0x,
                    bottomStart = dimensions().spacing0x,
                    topEnd = dimensions().textFieldCornerSize,
                    bottomEnd = dimensions().textFieldCornerSize,
                ),
                onTap = {
                    timePickerDialogState.show(Unit)
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(commonR.drawable.ic_arrow_drop_down),
                        contentDescription = null,
                        tint = colorsScheme().onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = dimensions().spacing4x, end = dimensions().spacing16x)
                            .size(dimensions().spacing16x)
                    )
                },
                modifier = Modifier.weight(1f),
                inputModifier = Modifier.clearAndSetSemantics {
                    contentDescription = timePlaceholder
                    role = Role.Button
                }
            )
        }
        AnimatedVisibility(visible = timeError != null) {
            Text(
                text = when (timeError) {
                    is NewMeetingState.TimeError.StartTimeInPastError -> stringResource(R.string.new_meeting_start_in_past_error)
                    is NewMeetingState.TimeError.EndTimeInPastError -> stringResource(R.string.new_meeting_end_in_past_error)
                    is NewMeetingState.TimeError.EndTimeBeforeStartTimeError -> stringResource(R.string.new_meeting_end_before_start_error)
                    else -> String.EMPTY
                },
                style = MaterialTheme.wireTypography.label04,
                textAlign = TextAlign.Start,
                color = colorsScheme().error,
                modifier = Modifier.padding(top = dimensions().spacing4x)
            )
        }
    }
    VisibilityState(status = datePickerDialogState) {
        WireDatePickerDialog(
            title = datePlaceholder,
            selectedDateMillis = time.toEpochMilliseconds(),
            selectableDates = FutureSelectableDates(),
            onDateSelected = { millis ->
                if (millis != null) {
                    val timeZone = TimeZone.currentSystemDefault()
                    val timeDateTime = time.toLocalDateTime(timeZone)
                    val dateDateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone)
                    val combinedDateTime = LocalDateTime(
                        year = dateDateTime.year,
                        monthNumber = dateDateTime.monthNumber,
                        dayOfMonth = dateDateTime.dayOfMonth,
                        hour = timeDateTime.hour,
                        minute = timeDateTime.minute,
                        second = 0,
                        nanosecond = 0
                    )
                    onTimeChanged(combinedDateTime.toInstant(timeZone))
                    datePickerDialogState.dismiss()
                }
            },
            onDismiss = datePickerDialogState::dismiss,
        )
    }
    VisibilityState(status = timePickerDialogState) {
        WireTimePickerDialog(
            title = timePlaceholder,
            selectedTime = time.toEpochMilliseconds().asTimePickerResult(),
            onTimeSelected = { timePickerResult ->
                val timeZone = TimeZone.currentSystemDefault()
                val dateDateTime = time.toLocalDateTime(timeZone)
                val combinedDateTime = LocalDateTime(
                    year = dateDateTime.year,
                    monthNumber = dateDateTime.monthNumber,
                    dayOfMonth = dateDateTime.dayOfMonth,
                    hour = timePickerResult.hour,
                    minute = timePickerResult.minute,
                    second = 0,
                    nanosecond = 0
                )
                onTimeChanged(combinedDateTime.toInstant(timeZone))
                timePickerDialogState.dismiss()
            },
            onDismiss = timePickerDialogState::dismiss,
        )
    }
}

@Composable
private fun RepeatingIntervalDropDown(
    repeatingInterval: MeetingItem.RepeatingInterval,
    onRepeatingIntervalChanged: (MeetingItem.RepeatingInterval) -> Unit,
    items: List<MeetingItem.RepeatingInterval> = MeetingItem.RepeatingInterval.entries,
) {
    val resources = LocalResources.current
    WireDropDown(
        items = remember(resources) {
            items.map { resources.getString(it.nameResId) }
        },
        defaultItemIndex = items.indexOf(repeatingInterval),
        label = stringResource(R.string.new_meeting_repeats_input_label).uppercase(),
        autoUpdateSelection = false,
        showDefaultTextIndicator = false,
        showSelectionFieldWhenExpanded = false,
        onChangeClickDescription = stringResource(R.string.content_description_new_meeting_repeating_options)
    ) { selectedIndex ->
        onRepeatingIntervalChanged(items[selectedIndex])
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewNewMeetingScreen_MeetNow() = WireTheme {
    NewMeetingContent(
        titleState = rememberTextFieldState("Meeting with 9 users"),
        type = NewMeetingType.MeetNow,
        state = NewMeetingState.initialState(CurrentTimeProvider.Preview).copy(
            confirmedContacts = buildContacts(names.size),
            continueButtonEnabled = true,
        ),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewNewMeetingScreen_Schedule() = WireTheme {
    NewMeetingContent(
        titleState = rememberTextFieldState(),
        type = NewMeetingType.Schedule,
        state = NewMeetingState.initialState(CurrentTimeProvider.Preview).copy(
            startTime = getNextFullHour(CurrentTimeProvider.Preview.invoke()),
            endTime = getNextFullHour(CurrentTimeProvider.Preview.invoke()).plus(1.hours),
            repeatingInterval = MeetingItem.RepeatingInterval.Weekly,
        ),
    )
}

private val names: List<String> = listOf(
    "Alice Smith", "Bob Johnson", "Charlie Brown", "David Wilson", "Eve Davis", "Frank Miller", "Grace Lee", "Hank Taylor", "Ivy Anderson"
)

private fun buildContacts(count: Int) = List(count) {
    Contact(
        id = "id_$it",
        domain = "domain",
        name = names[it % names.size],
        handle = names[it % names.size].lowercase().replace(" ", "."),
        label = names[it % names.size].lowercase().replace(" ", "."),
        membership = Membership.Standard,
        connectionState = ConnectionState.ACCEPTED,
    )
}.toPersistentSet()
