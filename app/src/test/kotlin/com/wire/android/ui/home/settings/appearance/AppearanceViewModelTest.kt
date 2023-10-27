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
 */
package com.wire.android.ui.home.settings.appearance

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.ui.theme.ThemeOption
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class AppearanceViewModelTest {

    @Test
    fun `given theme option, when changing it, then should update global data store`() {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        viewModel.selectThemeOption(ThemeOption.DARK)

        coVerify(exactly = 1) { arrangement.globalDataStore.setThemeOption(ThemeOption.DARK) }
    }

    private class Arrangement {
        @MockK
        lateinit var globalDataStore: GlobalDataStore

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { globalDataStore.setThemeOption(any()) } returns Unit
        }

        private val viewModel = AppearanceViewModel(globalDataStore)

        fun arrange() = this to viewModel
    }
}
