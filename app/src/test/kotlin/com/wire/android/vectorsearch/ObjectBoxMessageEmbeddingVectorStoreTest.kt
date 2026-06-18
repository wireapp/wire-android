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

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.objectbox.exception.DbSchemaException
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ObjectBoxMessageEmbeddingVectorStoreTest {

    @After
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

    @Test
    fun givenSchemaDowngradeException_whenCheckingRecovery_thenItIsRecoverable() {
        val exception = DbSchemaException("DB's last index ID 10 is higher than 6 from model")

        assertTrue(ObjectBoxMessageEmbeddingVectorStore.isRecoverableSchemaDowngrade(exception))
    }

    @Test
    fun givenUnrelatedSchemaException_whenCheckingRecovery_thenItIsNotRecoverable() {
        val exception = DbSchemaException("Property type does not match model")

        assertFalse(ObjectBoxMessageEmbeddingVectorStore.isRecoverableSchemaDowngrade(exception))
    }
}
