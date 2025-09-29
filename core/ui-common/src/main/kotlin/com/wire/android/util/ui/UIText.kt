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

package com.wire.android.util.ui

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wire.android.util.AnyPrimitiveAsStringSerializer
import com.wire.kalium.logic.data.message.mention.MessageMention
import kotlinx.serialization.Serializable

@Serializable
sealed class UIText {

    @Serializable
    data class DynamicString(
        val value: String,
        val mentions: List<MessageMention> = listOf()
    ) : UIText()

    @Serializable
    class StringResource(
        @StringRes val resId: Int,
        vararg val formatArgs: @Serializable(with = AnyPrimitiveAsStringSerializer::class) Any
    ) : UIText()

    @Serializable
    class PluralResource(
        @PluralsRes val resId: Int,
        val count: Int,
        vararg val formatArgs: @Serializable(with = AnyPrimitiveAsStringSerializer::class) Any
    ) : UIText()

    @Suppress("SpreadOperator")
    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(id = resId, *formatArgs)
        is PluralResource -> pluralStringResource(id = resId, count, *formatArgs)
    }

    @Suppress("SpreadOperator")
    fun asString(resources: Resources): String = when (this) {
        is DynamicString -> value
        is StringResource -> resources.getString(resId, *formatArgs)
        is PluralResource -> resources.getQuantityString(resId, count, *formatArgs)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        return when (this) {
            is DynamicString -> {
                other as DynamicString
                value == other.value && mentions == other.mentions
            }

            is StringResource -> {
                other as StringResource
                resId == other.resId && formatArgs.contentEquals(other.formatArgs)
            }

            is PluralResource -> {
                other as PluralResource
                resId == other.resId && count == other.count &&
                        formatArgs.contentEquals(other.formatArgs)
            }
        }
    }

    override fun hashCode(): Int {
        var result = javaClass.hashCode()
        when (this) {
            is DynamicString -> {
                result += value.hashCode()
                result += mentions.hashCode()
            }

            is StringResource -> {
                result += resId
                result += formatArgs.contentHashCode()
            }

            is PluralResource -> {
                result += resId
                result += count
                result += formatArgs.contentHashCode()
            }
        }
        return result
    }
}

fun String.toUIText() = UIText.DynamicString(this)
