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
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.datastore

import com.wire.android.feature.aiassistant.GlobalDataStoreAiModelSelectionStore
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GlobalDataStoreAiModelSelectionStoreTest {

    @Test
    fun givenNoSelectedModelId_whenReading_thenNullIsReturned() = runTest {
        val arrangement = Arrangement()
            .withSelectedAiModelId(null)
            .arrange()

        assertEquals(null, arrangement.store.getSelectedModelId())
    }

    @Test
    fun givenSelectedModelId_whenReading_thenStoredValueIsReturned() = runTest {
        val arrangement = Arrangement()
            .withSelectedAiModelId("google/test-model")
            .arrange()

        assertEquals("google/test-model", arrangement.store.getSelectedModelId())
    }

    @Test
    fun givenModelId_whenWriting_thenValueIsDelegatedToGlobalDataStore() = runTest {
        val arrangement = Arrangement().arrange()

        arrangement.store.setSelectedModelId("google/test-model")

        coVerify(exactly = 1) { arrangement.globalDataStore.setSelectedAiModelId("google/test-model") }
    }

    private class Arrangement {
        @MockK
        lateinit var globalDataStore: GlobalDataStore

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withSelectedAiModelId(modelId: String?) = apply {
            coEvery { globalDataStore.getSelectedAiModelId() } returns modelId
        }

        fun arrange() = Result(
            store = GlobalDataStoreAiModelSelectionStore(globalDataStore),
            globalDataStore = globalDataStore
        )
    }

    private data class Result(
        val store: GlobalDataStoreAiModelSelectionStore,
        val globalDataStore: GlobalDataStore
    )
}
