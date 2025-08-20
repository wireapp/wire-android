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
package com.wire.android.ui.common.bottomsheet

import androidx.compose.runtime.saveable.SaverScope
import com.wire.android.util.AnyPrimitiveAsStringSerializer
import io.mockk.mockk
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf

class WireModalSheetStateTest {

    @Suppress("LongParameterList")
    @Serializable
    class SerializableTestModel(
        val boolean: Boolean,
        val int: Int,
        val long: Long,
        val float: Float,
        val double: Double,
        val char: Char,
        val string: String,
        val nullable: String?,
        val list: List<String>,
        vararg val any: @Serializable(with = AnyPrimitiveAsStringSerializer::class) Any
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SerializableTestModel) return false

            if (boolean != other.boolean) return false
            if (int != other.int) return false
            if (long != other.long) return false
            if (float != other.float) return false
            if (double != other.double) return false
            if (char != other.char) return false
            if (string != other.string) return false
            if (nullable != other.nullable) return false
            if (list != other.list) return false
            if (!any.contentEquals(other.any)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = boolean.hashCode()
            result = 31 * result + int
            result = 31 * result + long.hashCode()
            result = 31 * result + float.hashCode()
            result = 31 * result + double.hashCode()
            result = 31 * result + char.hashCode()
            result = 31 * result + string.hashCode()
            result = 31 * result + (nullable?.hashCode() ?: 0)
            result = 31 * result + list.hashCode()
            result = 31 * result + any.contentHashCode()
            return result
        }
    }

    @Test
    fun givenSerializableModel_whenSavingState_thenStateIsSavedAndRestoredProperly() {
        // given
        val model = SerializableTestModel(
            boolean = true,
            int = 1,
            long = 2L,
            float = 3.0f,
            double = 4.0,
            char = 'c',
            string = "string",
            nullable = null,
            list = listOf("a", "b", "c"),
            any = arrayOf(1, 2L, 3.0, 4f, true, false, 'c', "string")
        )
        val sheetValue = WireSheetValue.Expanded(model)
        with(WireModalSheetState.saver<SerializableTestModel>(mockk(), mockk(), mockk(), mockk())) {
            // when
            val saved = SaverScope { true }.save(WireModalSheetState(mockk(), mockk(), mockk(), mockk(), sheetValue))
            // then
            assertInstanceOf<SavedData>(saved).let {
                val restored = restore(it)
                assertInstanceOf<WireSheetValue.Expanded<SerializableTestModel>>(restored?.currentValue).let {
                    assertEquals(sheetValue.value, it.value)
                }
            }
        }
    }
}
