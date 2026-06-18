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
package com.wire.android.vectorsearch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.io.File

class ObjectBoxMessageEmbeddingVectorStoreTest {

    @AfterEach
    fun tearDown() {
        ObjectBoxMessageEmbeddingVectorStore.closeAllForTests()
    }

    @Test
    fun givenSameDirectory_whenCreatingStoreTwice_thenOpenStoreIsReused() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.filesDir, "objectbox-store-reuse-test")

        val first = ObjectBoxMessageEmbeddingVectorStore.create(context, directory)
        val second = ObjectBoxMessageEmbeddingVectorStore.create(context, directory)

        assertSame(first, second)
    }
}
