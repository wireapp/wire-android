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

package com.wire.android.navigation

import androidx.navigation.NavHostController
import com.wire.android.feature.NavigationSwitchAccountActions
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AccountSwitchNavigationTest {

    @Test
    fun givenNavigationGraphIsNotSet_whenAccountSwitchActionNavigates_thenReproduceStoreCrash() {
        val navController = NavHostController(mockk(relaxed = true))
        val navigator = Navigator(finish = {}, navController = navController)
        val actions = NavigationSwitchAccountActions(
            navigate = navigator::navigate,
            canUseNewLogin = { true },
        )

        val exception = assertThrows<IllegalArgumentException> {
            actions.noOtherAccountToSwitch()
        }

        assertTrue(exception.message.orEmpty().contains("Navigation graph has not been set"))
    }
}
