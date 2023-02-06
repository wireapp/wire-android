/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.settings.devices

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.navigation.NavigationManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class SelfDevicesViewModelTest {

    @Test
    fun `given a navigation request, to go back into previous screen, should go back`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement().arrange()

        // when
        viewModel.navigateBack()

        // then
        coVerify { arrangement.navigationManager.navigateBack() }
    }

    private class Arrangement {
        @MockK
        lateinit var navigationManager: NavigationManager

        private val viewModel by lazy {
            SelfDevicesViewModel(
                navigationManager = navigationManager
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            coEvery { navigationManager.navigateBack() } returns Unit
        }

        fun arrange() = this to viewModel
    }
}
