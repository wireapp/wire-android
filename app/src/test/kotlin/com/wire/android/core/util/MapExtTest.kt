package com.wire.android.core.util

import com.wire.android.UnitTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame

import org.junit.Test

class MapExtTest : UnitTest() {

    @Test
    fun `given a map with values, when spreading true as the intermediate key, then it should only have true as intermediate keys`() {
        val initialMap = mapOf(1 to "Hello", 2 to "World")

        val intermediateKeys = initialMap.spread { _, _ -> true }
            .values.flatMap { it.keys }

        intermediateKeys.size shouldBeEqualTo 2
        intermediateKeys shouldContainSame listOf(true, true)
    }

    @Test
    fun `given a map with values, when spreading the intermediate key, then it should have the spread intermediate keys`() {
        val initialMap = mapOf(1 to "Hello", 2 to "World")

        val intermediateKeys = initialMap.spread { _, value -> value.first() }
            .values.flatMap { it.keys }

        intermediateKeys.size shouldBeEqualTo 2
        intermediateKeys shouldContainSame listOf('H', 'W')
    }

    @Test
    fun `given a map with values, when spreading, then it should preserve the values in the internal mapping`() {
        val initialMap = mapOf(1 to "Hello", 2 to "World")

        val values = initialMap.spread { _, _ -> true }
            .values.flatMap { it.values }

        values.size shouldBeEqualTo 2
        values shouldContainSame listOf("Hello", "World")
    }

    @Test
    fun `given a map with values, when spreading, then it should preserve the original keys`() {
        val initialMap = mapOf(1 to "Hello", 2 to "World")

        val keys = initialMap.spread { _, _ -> true }.keys

        keys.size shouldBeEqualTo 2
        keys shouldContainSame listOf(1, 2)
    }

    @Test
    fun `given an empty map, when spreading, then it should return an empty map`() {
        val initialMap = mapOf<Int, String>()

        val spread = initialMap.spread { _, _ -> true }

        spread.size shouldBeEqualTo 0
    }
}
