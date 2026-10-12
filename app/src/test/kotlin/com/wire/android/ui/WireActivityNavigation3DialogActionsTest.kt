/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui

import android.content.Context
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class WireActivityNavigation3DialogActionsTest {
    @Test
    fun `logout forwards chosen clear data option and retains legacy hard logout`() {
        val viewModel = mockk<WireActivityViewModel>(relaxed = true)
        val switchActions = mockk<SwitchAccountActions>()
        val actions = navigation3DialogActions(
            runtime = mockk<WireNavigation3Runtime>(),
            switchAccountActions = switchActions,
            dependencies = WireActivityDialogActionDependencies(
                context = mockk<Context>(),
                viewModel = viewModel,
                updateApp = {},
                startTeamAppLock = {},
            ),
        )

        actions.logout(false)
        verify(exactly = 1) { viewModel.doHardLogout(any(), switchActions, false) }
        actions.logout(true)
        actions.hardLogout()
        verify(exactly = 2) { viewModel.doHardLogout(any(), switchActions, true) }
    }
}
