/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android

import org.junit.jupiter.api.Assertions
import kotlin.random.Random

val charPoolWithNumbers: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
val charPool: List<Char> = ('a'..'z') + ('A'..'Z')

fun Random.stringWithNumbers(length: Int) = (1..length)
    .map { Random.nextInt(0, charPoolWithNumbers.size).let { charPoolWithNumbers[it] } }
    .joinToString("")

fun Random.string(length: Int) = (1..length)
    .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
    .joinToString("")

inline fun <reified T> assertIs(actualValue: Any): T = Assertions.assertInstanceOf(T::class.java, actualValue)
