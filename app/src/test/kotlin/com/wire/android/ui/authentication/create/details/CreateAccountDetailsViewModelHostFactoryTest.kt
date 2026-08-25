/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.details

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.navigation.routes.auth.toAuthenticationServerLinks
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class CreateAccountDetailsViewModelHostFactoryTest {

    @Test
    fun `gateway maps valid and invalid results and forwards exact password`() = runTest {
        val validatePassword = mockk<ValidatePasswordUseCase>()
        val gateway = KaliumCreateAccountDetailsGateway(validatePassword)
        every { validatePassword("Valid1!") } returns ValidatePasswordResult.Valid
        every { validatePassword("invalid") } returns ValidatePasswordResult.Invalid()

        assertTrue(gateway.isPasswordValid("Valid1!"))
        assertFalse(gateway.isPasswordValid("invalid"))
        verify(exactly = 1) { validatePassword("Valid1!") }
        verify(exactly = 1) { validatePassword("invalid") }
    }

    @Test
    fun `host factory keeps custom links optional and selects effective links`() {
        val factory = factory(defaultServerConfig = ServerConfig.STAGING)

        val custom = factory.create(
            CreateAccountRouteFlowType.PERSONAL,
            ServerConfig.PRODUCTION.toAuthenticationServerLinks(),
        )
        val default = factory.create(CreateAccountRouteFlowType.PERSONAL, null)

        assertEquals(ServerConfig.PRODUCTION, custom.customServerConfig)
        assertEquals(ServerConfig.PRODUCTION, custom.serverConfig)
        assertNull(default.customServerConfig)
        assertEquals(ServerConfig.STAGING, default.serverConfig)
    }

    @Test
    fun `host factory requires team name only for team flow`() = runTest {
        val factory = factory()
        val personal = factory.create(CreateAccountRouteFlowType.PERSONAL, null)
        val team = factory.create(CreateAccountRouteFlowType.TEAM, null)
        advanceUntilIdle()

        listOf(personal, team).forEach(::fillPersonalFields)
        advanceUntilIdle()

        assertTrue(personal.detailsState.continueEnabled)
        assertFalse(team.detailsState.continueEnabled)

        team.teamNameTextState.setTextAndPlaceCursorAtEnd("Wire Team")
        advanceUntilIdle()
        assertTrue(team.detailsState.continueEnabled)
    }

    private fun factory(
        defaultServerConfig: ServerConfig.Links = ServerConfig.STAGING,
    ): CreateAccountDetailsViewModelHostFactory {
        val validatePassword = mockk<ValidatePasswordUseCase>()
        every { validatePassword(any()) } returns ValidatePasswordResult.Valid
        return CreateAccountDetailsViewModelHostFactory(validatePassword, defaultServerConfig)
    }

    private fun fillPersonalFields(viewModel: CreateAccountDetailsViewModel<ServerConfig.Links, *>) {
        viewModel.firstNameTextState.setTextAndPlaceCursorAtEnd("Ada")
        viewModel.lastNameTextState.setTextAndPlaceCursorAtEnd("Lovelace")
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("Valid1!")
        viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("Valid1!")
    }
}
