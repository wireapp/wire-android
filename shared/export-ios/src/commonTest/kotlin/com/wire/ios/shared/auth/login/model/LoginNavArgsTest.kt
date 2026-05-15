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
package com.wire.ios.shared.auth.login.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginNavArgsTest {
    @Test
    fun givenNoUserIdentifier_whenCheckingEditable_thenItIsEditable() {
        assertTrue(LoginUserIdentifier.None.userIdentifierEditable)
    }

    @Test
    fun givenPreFilledUserIdentifierWithDefaultEditable_whenCheckingEditable_thenItIsNotEditable() {
        val userIdentifier = LoginUserIdentifier.PreFilled(value = "user@example.com")

        assertFalse(userIdentifier.userIdentifierEditable)
    }

    @Test
    fun givenPreFilledUserIdentifierWithEditableTrue_whenCheckingEditable_thenItIsEditable() {
        val userIdentifier = LoginUserIdentifier.PreFilled(value = "user@example.com", editable = true)

        assertTrue(userIdentifier.userIdentifierEditable)
    }
}
