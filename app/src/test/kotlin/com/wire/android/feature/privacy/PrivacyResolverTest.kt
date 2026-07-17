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
package com.wire.android.feature.privacy

import com.wire.android.feature.privacy.model.ConversationPrivacyLevel
import com.wire.android.feature.privacy.model.EffectivePrivacyLevel
import com.wire.android.feature.privacy.model.PrivacyResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrivacyResolverTest {

    @Test
    fun givenNormal_whenResolvingWithoutPanic_thenNormal() {
        assertEquals(EffectivePrivacyLevel.NORMAL, PrivacyResolver.resolve(ConversationPrivacyLevel.NORMAL, false))
    }

    @Test
    fun givenNormal_whenResolvingWithPanic_thenStillNormal() {
        // Panic must NEVER escalate normal conversations (spec).
        assertEquals(EffectivePrivacyLevel.NORMAL, PrivacyResolver.resolve(ConversationPrivacyLevel.NORMAL, true))
    }

    @Test
    fun givenSensitive_whenResolvingWithoutPanic_thenSensitive() {
        assertEquals(EffectivePrivacyLevel.SENSITIVE, PrivacyResolver.resolve(ConversationPrivacyLevel.SENSITIVE, false))
    }

    @Test
    fun givenSensitive_whenResolvingWithPanic_thenSensitivePanic() {
        assertEquals(EffectivePrivacyLevel.SENSITIVE_PANIC, PrivacyResolver.resolve(ConversationPrivacyLevel.SENSITIVE, true))
    }

    @Test
    fun givenHighlySensitive_whenResolvingWithoutPanic_thenHighlySensitive() {
        assertEquals(
            EffectivePrivacyLevel.HIGHLY_SENSITIVE,
            PrivacyResolver.resolve(ConversationPrivacyLevel.HIGHLY_SENSITIVE, false)
        )
    }

    @Test
    fun givenHighlySensitive_whenResolvingWithPanic_thenHighlySensitivePanic() {
        assertEquals(
            EffectivePrivacyLevel.HIGHLY_SENSITIVE_PANIC,
            PrivacyResolver.resolve(ConversationPrivacyLevel.HIGHLY_SENSITIVE, true)
        )
    }

    @Test
    fun givenEffectiveLevels_whenCheckingFlags_thenFlagsAreConsistentWithSpec() {
        // requiresAuth only for the highly-sensitive family
        assertFalse(EffectivePrivacyLevel.NORMAL.requiresAuth)
        assertFalse(EffectivePrivacyLevel.SENSITIVE.requiresAuth)
        assertFalse(EffectivePrivacyLevel.SENSITIVE_PANIC.requiresAuth)
        assertTrue(EffectivePrivacyLevel.HIGHLY_SENSITIVE.requiresAuth)
        assertTrue(EffectivePrivacyLevel.HIGHLY_SENSITIVE_PANIC.requiresAuth)

        // previews hidden for everything but normal
        assertFalse(EffectivePrivacyLevel.NORMAL.hidesPreviewInList)
        assertTrue(EffectivePrivacyLevel.SENSITIVE.hidesPreviewInList)
        assertTrue(EffectivePrivacyLevel.HIGHLY_SENSITIVE.hidesPreviewInList)

        // identity hidden only for highly-sensitive family
        assertFalse(EffectivePrivacyLevel.SENSITIVE.hidesIdentityInList)
        assertTrue(EffectivePrivacyLevel.HIGHLY_SENSITIVE.hidesIdentityInList)

        // secure window for everything but normal
        assertFalse(EffectivePrivacyLevel.NORMAL.needsSecureWindow)
        assertTrue(EffectivePrivacyLevel.SENSITIVE.needsSecureWindow)
    }
}
