/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.meetings.ui.create

import androidx.compose.runtime.Composable
import com.wire.android.feature.meetings.ui.newMeetingViewModel
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.common.HandleActions
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireViewModelOwner

/** Host actions used by entries inside the Navigation 3 meeting flow. */
interface MeetingsNavigation3Actions {
    fun exitMeetingFlow()
    fun openUserProfile(userId: MeetingParticipantId)
}

@kotlinx.serialization.Serializable
data class MeetingParticipantId(val value: String, val domain: String) {
    init {
        require(value.isNotBlank())
        require(domain.isNotBlank())
    }
}

object MeetingsNavigation3Contribution {
    const val ROUTE_REGISTRATION_COUNT: Int = 2

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: MeetingsNavigation3Actions,
    ): List<WireEntryProviderInstaller> =
        listOf(meetingsNavigation3Entries(runtime, actions))
}

fun meetingsNavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: MeetingsNavigation3Actions,
): WireEntryProviderInstaller = {
    wireEntry<NewMeetingDetailsRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        NewMeetingDetailsNavigation3Entry(route, runtime, actions)
    }
    wireEntry<NewMeetingParticipantsRoute>(presentation = WireEntryPresentation.PopUp) {
        NewMeetingParticipantsNavigation3Entry(it, runtime, actions)
    }
}

@Composable
private fun NewMeetingDetailsNavigation3Entry(
    route: NewMeetingDetailsRoute,
    runtime: WireNavigation3Runtime,
    actions: MeetingsNavigation3Actions,
) {
    val viewModel = newMeetingFlowViewModel(route.type, route.meetingId, route.flowId)
    val navigateBack = { if (!runtime.navigator.goBack()) actions.exitMeetingFlow() }
    NewMeetingContent(
        type = route.type.toLegacyType(route.meetingId),
        onBackPressed = navigateBack,
        state = viewModel.state,
        titleState = viewModel.titleTextState,
        onParticipantsClicked = {
            runtime.navigator.navigate(
                WireNavigationCommand(
                    NewMeetingParticipantsRoute(route.sessionId, route.flowId, route.type, route.meetingId)
                )
            )
        },
        onCreateClicked = viewModel::submitCreation,
        onUpdateClicked = viewModel::submitUpdate,
        onStartTimeChanged = viewModel::updateStartTime,
        onEndTimeChanged = viewModel::updateEndTime,
        onRepeatingIntervalChanged = viewModel::updateRepeatingInterval,
    )
    viewModel.state.submitError?.let { submitError ->
        NewMeetingErrorDialog(
            error = submitError,
            type = viewModel.type,
            isSubmitting = viewModel.state.isSubmitting,
            onDismiss = viewModel::dismissCreationError,
            onRetryUpdateConversationName = viewModel::retryUpdateConversationName,
        )
    }
    if (viewModel.state.initialLoading == NewMeetingState.InitialLoadingState.Error) {
        FailedToLoadEditMeetingDataError(navigateBack)
    }
    HandleActions(viewModel.actions) {
        navigateBack()
    }
}

@Composable
private fun NewMeetingParticipantsNavigation3Entry(
    route: NewMeetingParticipantsRoute,
    runtime: WireNavigation3Runtime,
    actions: MeetingsNavigation3Actions,
) {
    NewMeetingParticipantsRouteContent(
        newMeetingViewModel = newMeetingFlowViewModel(route.type, route.meetingId, route.flowId),
        onNavigateBack = runtime.navigator::goBack,
        onOpenUserProfile = {
            actions.openUserProfile(MeetingParticipantId(it.value, it.domain))
        },
    )
}

@Composable
private fun newMeetingFlowViewModel(
    type: NewMeetingRouteType,
    meetingId: com.wire.kalium.logic.data.id.MeetingId?,
    flowId: String,
): NewMeetingViewModel {
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(flowId))
    return newMeetingViewModel(
        navArgs = NewMeetingNavArgs(type.toLegacyType(meetingId)),
        viewModelStoreOwner = flowOwner,
    )
}
