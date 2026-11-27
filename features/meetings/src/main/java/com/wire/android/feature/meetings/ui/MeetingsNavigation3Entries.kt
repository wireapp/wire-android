/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.meetings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import com.wire.android.feature.meetings.ui.create.FailedToLoadEditMeetingDataError
import com.wire.android.feature.meetings.ui.create.NewMeetingContent
import com.wire.android.feature.meetings.ui.create.NewMeetingErrorDialog
import com.wire.android.feature.meetings.ui.create.NewMeetingNavArgs
import com.wire.android.feature.meetings.ui.create.NewMeetingParticipantsRouteContent
import com.wire.android.feature.meetings.ui.create.NewMeetingState
import com.wire.android.feature.meetings.ui.create.NewMeetingViewActions
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModel
import com.wire.android.feature.meetings.ui.link.CreateMeetingLinkScreen
import com.wire.android.feature.meetings.ui.link.MeetingLinkScreen
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireViewModelOwner

val NewMeetingNavigation3ResultType = WireNavigation3ResultType(
    contract = NewMeetingResultContract,
    serializer = NewMeetingResult.serializer(),
)

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
    const val ROUTE_REGISTRATION_COUNT: Int = 4
    val resultTypes: List<WireNavigation3ResultType<*>> = listOf(NewMeetingNavigation3ResultType)

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
    wireEntry<CreateMeetingLinkRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        CreateMeetingLinkScreen(
            meetingId = route.meetingId,
            onBack = { if (!runtime.navigator.goBack()) actions.exitMeetingFlow() },
        )
    }
    wireEntry<MeetingLinkRoute>(presentation = WireEntryPresentation.PopUp) { route ->
        MeetingLinkScreen(
            meetingId = route.meetingId,
            mode = route.mode,
            onCreateNewLink = {
                runtime.navigator.navigate(WireNavigationCommand(CreateMeetingLinkRoute(route.sessionId, route.meetingId)))
            },
            onBack = { if (!runtime.navigator.goBack()) actions.exitMeetingFlow() },
        )
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
    AnimatedContent(
        targetState = viewModel.state,
        contentKey = { it::class.simpleName },
        label = "NewMeetingDetailsState",
    ) { state ->
        when (state) {
            NewMeetingState.Loading -> CenteredCircularProgressBarIndicator()
            NewMeetingState.Error -> FailedToLoadEditMeetingDataError(navigateBack)
            is NewMeetingState.Ready -> {
                NewMeetingContent(
                    type = route.type.toLegacyType(route.meetingId),
                    onBackPressed = navigateBack,
                    state = state,
                    titleState = viewModel.titleTextState,
                    passwordState = viewModel.passwordTextState,
                    confirmPasswordState = viewModel.confirmPasswordTextState,
                    onGenerateRandomPassword = viewModel::onGenerateRandomPassword,
                    hasUnsavedChanges = viewModel.hasUnsavedChanges(),
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
                state.submitError?.let { submitError ->
                    NewMeetingErrorDialog(
                        error = submitError,
                        type = viewModel.type,
                        isSubmitting = state.isSubmitting,
                        onDismiss = viewModel::dismissCreationError,
                        onRetryUpdateConversationName = viewModel::retryUpdateConversationName,
                    )
                }
            }
        }
    }
    HandleActions(viewModel.actions) { action ->
        when (action) {
            is NewMeetingViewActions.Success ->
                if (!runtime.completeCurrentAndPop(
                        resultType = NewMeetingNavigation3ResultType,
                        result = WireNavResult.Value(action.result)
                    )
                ) {
                    navigateBack()
                }
        }
    }
}

@Composable
private fun NewMeetingParticipantsNavigation3Entry(
    route: NewMeetingParticipantsRoute,
    runtime: WireNavigation3Runtime,
    actions: MeetingsNavigation3Actions,
) {
    val viewModel = newMeetingFlowViewModel(route.type, route.meetingId, route.flowId)
    val navigateBack = { if (!runtime.navigator.goBack()) actions.exitMeetingFlow() }
    AnimatedContent(
        targetState = viewModel.state,
        contentKey = { it::class.simpleName },
        label = "NewMeetingParticipantsState",
    ) { state ->
        when (state) {
            NewMeetingState.Loading -> CenteredCircularProgressBarIndicator()
            NewMeetingState.Error -> FailedToLoadEditMeetingDataError(navigateBack)
            is NewMeetingState.Ready -> NewMeetingParticipantsRouteContent(
                state = state,
                onClose = {
                    viewModel.resetSelectedContacts()
                    navigateBack()
                },
                onSelect = {
                    viewModel.confirmSelectedContacts()
                    navigateBack()
                },
                onOpenUserProfile = {
                    actions.openUserProfile(MeetingParticipantId(it.value, it.domain))
                },
                onEmailChecked = viewModel::updateSelectedEmail,
                onContactChecked = viewModel::updateSelectedContact,
                onGroupChecked = viewModel::updateSelectedGroup,
            )
        }
    }
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
