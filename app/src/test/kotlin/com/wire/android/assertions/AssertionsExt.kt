/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.assertions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.reflect.KClass

// Migrated and similar DSL from kluent to use with JUnit assertions.

inline fun <reified T> Any?.assertInstanceOf(): T {
    return assertInstanceOf(T::class.java, this)
}

infix fun <T> T.shouldBeEqualTo(expected: T?): T = this.apply { assertEquals(expected, this) }

infix fun <T> T.shouldNotBeEqualTo(expected: T?) = this.apply { assertNotEquals(expected, this) }

infix fun Any?.shouldBeInstanceOf(className: Class<*>) =
    assertTrue(className.isInstance(this), "Expected $this to be an instance of $className")

infix fun Any?.shouldBeInstanceOf(className: KClass<*>) =
    assertTrue(className.isInstance(this), "Expected $this to be an instance of $className")

inline fun <reified T> Any?.shouldBeInstanceOf(): T =
    if (this is T) this else throw AssertionError("Expected $this to be an instance or subclass of ${T::class.qualifiedName}")

infix fun Any?.shouldNotBeInstanceOf(className: Class<*>) =
    assertFalse(className.isInstance(this), "Expected $this to not be an instance of $className")

infix fun Any?.shouldNotBeInstanceOf(className: KClass<*>) =
    assertFalse(className.isInstance(this), "Expected $this to not be an instance of $className")

inline fun <reified T> Any?.shouldNotBeInstanceOf() = apply {
    if (this is T) {
        throw AssertionError("Expected $this to not be an instance or subclass of ${T::class.qualifiedName}")
    }
}

infix fun <T> Collection<T>.shouldHaveSize(expectedSize: Int) =
    assertTrue(expectedSize == this.count(), "Expected $this to have size $expectedSize, it was ${count()}")
