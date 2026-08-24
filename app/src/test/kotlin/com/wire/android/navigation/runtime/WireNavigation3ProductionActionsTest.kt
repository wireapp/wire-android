/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.runtime

import com.wire.android.feature.cells.navigation.ConversationFilesRoute
import com.wire.android.feature.meetings.ui.create.NewMeetingDetailsRoute
import com.wire.android.feature.meetings.ui.create.NewMeetingRouteType
import com.wire.android.feature.meetings.ui.create.NewMeetingType
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.routes.auth.AuthenticationLoginCompletion
import com.wire.android.navigation.routes.auth.AuthenticationNavigation3Router
import com.wire.android.navigation.routes.auth.CreateAccountUsernameRoute
import com.wire.android.navigation.routes.auth.NewLoginRoute
import com.wire.android.navigation.routes.media.ConversationMediaRoute
import com.wire.android.navigation.routes.media.MediaConversationId
import com.wire.android.navigation.routes.utility.DebugRoute
import com.wire.android.navigation.routes.auth.InitialSyncRoute
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceRoute
import com.wire.android.ui.authentication.devices.register.RegisterDeviceRoute
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentRoute
import com.wire.android.ui.home.HomeRequirement
import com.wire.android.ui.home.conversations.ConversationRoute
import com.wire.android.ui.home.conversations.details.ConversationDetailsId
import com.wire.android.ui.home.settings.SettingsNavigation3Destination
import com.wire.android.ui.settings.devices.SelfDevicesRoute
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireNavigationController
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class WireNavigation3ProductionActionsTest {

    @Test
    fun `given every login completion, when resolving, then target is typed and whole stack is cleared`() {
        val cases = listOf(
            AuthenticationLoginCompletion.Home(OtherSession) to HomeRoute::class.java,
            AuthenticationLoginCompletion.InitialSync(OtherSession) to InitialSyncRoute::class.java,
            AuthenticationLoginCompletion.E2EIEnrollment(OtherSession) to E2EIEnrollmentRoute::class.java,
            AuthenticationLoginCompletion.RemoveDevice(OtherSession) to RemoveDeviceRoute::class.java,
        )

        cases.forEach { (completion, expectedType) ->
            val command = WireNavigation3ProductionCommandResolver.completeLogin(completion)

            assertInstanceOf(expectedType, command.destination)
            assertEquals(WireBackStackMode.CLEAR_WHOLE, command.backStackMode)
        }
    }

    @Test
    fun `given session carried by device completion, when resolving, then completion uses that session`() {
        val command = WireNavigation3ProductionCommandResolver.completeLogin(
            AuthenticationLoginCompletion.RemoveDevice(OtherSession),
        )

        assertEquals(OtherSession, (command.destination as RemoveDeviceRoute).sessionId)
    }

    @Test
    fun `given login completion carries a new session, when resolving, then that session is used`() {
        val command = WireNavigation3ProductionCommandResolver.completeLogin(
            AuthenticationLoginCompletion.Home(OtherSession),
        )

        assertEquals(OtherSession, (command.destination as HomeRoute).sessionId)
    }

    @Test
    fun `given home requirements, when resolving, then legacy clear whole semantics are preserved`() {
        val register = WireNavigation3ProductionCommandResolver.homeRequirement(
            HomeRequirement.RegisterDevice(UserId("other", "wire.test")),
            currentSessionId = null,
        )
        val username = WireNavigation3ProductionCommandResolver.homeRequirement(
            HomeRequirement.CreateAccountUsername,
            currentSessionId = Session,
        )

        assertInstanceOf(RegisterDeviceRoute::class.java, register.destination)
        assertEquals(Session, (username.destination as CreateAccountUsernameRoute).sessionId)
        assertEquals(WireBackStackMode.CLEAR_WHOLE, register.backStackMode)
        assertEquals(WireBackStackMode.CLEAR_WHOLE, username.backStackMode)
    }

    @Test
    fun `given settings target, when resolving, then app owned targets use typed routes and external targets return null`() {
        val devices = WireNavigation3ProductionCommandResolver.settings(
            SettingsNavigation3Destination.MANAGE_DEVICES,
            Session,
        )
        val debug = WireNavigation3ProductionCommandResolver.settings(
            SettingsNavigation3Destination.DEBUG_SETTINGS,
            Session,
        )
        val support = WireNavigation3ProductionCommandResolver.settings(
            SettingsNavigation3Destination.SUPPORT,
            Session,
        )

        assertInstanceOf(SelfDevicesRoute::class.java, devices?.destination)
        assertInstanceOf(DebugRoute::class.java, debug?.destination)
        assertNull(support)
    }

    @Test
    fun `given imported share, when opening conversation, then share entry is replaced without clearing parent stack`() {
        val command = WireNavigation3ProductionCommandResolver.conversationFromShare(
            Session,
            MediaConversationId("conversation", "wire.test"),
            assets = emptyList(),
            text = "shared text",
        )

        assertInstanceOf(ConversationRoute::class.java, command.destination)
        assertEquals(WireBackStackMode.REMOVE_CURRENT_AND_REPLACE, command.backStackMode)
    }

    @Test
    fun `given meeting type, when opening meeting flow, then typed details route is navigated directly`() {
        val (actions, navigator) = productionActions()
        val command = slot<WireNavigationCommand>()
        every { navigator.navigate(capture(command)) } returns true

        listOf(
            NewMeetingType.MeetNow to NewMeetingRouteType.MEET_NOW,
            NewMeetingType.Schedule to NewMeetingRouteType.SCHEDULE,
        ).forEach { (legacyType, expectedType) ->
            actions.openNewMeeting(legacyType)

            val route = assertInstanceOf(NewMeetingDetailsRoute::class.java, command.captured.destination)
            assertEquals(Session, route.sessionId)
            assertEquals(expectedType, route.type)
            assertEquals(WireBackStackMode.NONE, command.captured.backStackMode)
        }
    }

    @Test
    fun `given Cells conversation, when opening details media action, then Shared Drive is opened`() {
        val (actions, navigator) = productionActions()
        val command = slot<WireNavigationCommand>()
        every { navigator.navigate(capture(command)) } returns true

        actions.openConversationMedia(
            conversationId = ConversationDetailsId("conversation", "wire.test"),
            isCellsConversation = true,
            groupName = "Project",
        )

        val route = assertInstanceOf(ConversationFilesRoute::class.java, command.captured.destination)
        assertEquals(Session, route.sessionId)
        assertEquals("conversation@wire.test", route.args.conversationId)
        assertEquals(listOf("Project"), route.args.breadcrumbs)
    }

    @Test
    fun `given regular conversation, when opening details media action, then Media is opened`() {
        val (actions, navigator) = productionActions()
        val command = slot<WireNavigationCommand>()
        every { navigator.navigate(capture(command)) } returns true

        actions.openConversationMedia(
            conversationId = ConversationDetailsId("conversation", "wire.test"),
            isCellsConversation = false,
            groupName = "Project",
        )

        val route = assertInstanceOf(ConversationMediaRoute::class.java, command.captured.destination)
        assertEquals(Session, route.sessionId)
        assertEquals(MediaConversationId("conversation", "wire.test"), route.conversationId)
    }

    @Test
    fun `given new login is already visible, when no other account remains, then navigation is not repeated`() {
        val (actions, navigator) = productionActions(
            currentRoute = NewLoginRoute.start(),
            useNewLogin = true,
        )

        actions.noOtherAccountToSwitch()

        verify(exactly = 0) { navigator.navigate(any()) }
    }

    private fun productionActions(
        currentRoute: WireRoute = HomeRoute(Session),
        useNewLogin: Boolean = false,
    ): Triple<WireNavigation3ProductionActions, WireNavigationController, WireNavigation3Runtime> {
        val runtime = mockk<WireNavigation3Runtime>()
        val navigator = mockk<WireNavigationController>()
        val loginTypeSelector = mockk<LoginTypeSelector>()
        every { runtime.navigator } returns navigator
        every { navigator.currentRoute } returns currentRoute
        every { loginTypeSelector.canUseNewLogin() } returns useNewLogin
        val actions = WireNavigation3ProductionActions(
            runtime = runtime,
            activity = mockk(relaxed = true),
            currentSessionId = { Session },
            loginTypeSelector = loginTypeSelector,
            authenticationRouter = AuthenticationNavigation3Router(runtime),
        )
        return Triple(actions, navigator, runtime)
    }

    private companion object {
        val Session = WireSessionId("self", "wire.test")
        val OtherSession = WireSessionId("other", "wire.test")
    }
}
