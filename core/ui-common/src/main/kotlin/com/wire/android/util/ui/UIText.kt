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
import com.wire.kalium.logic.data.message.linkpreview.MessageLinkPreview
import com.wire.kalium.logic.data.message.mention.MessageMention
import kotlinx.serialization.Serializable

@Serializable
sealed class UIText {

    @Serializable
    data class DynamicString(
        val value: String,
        // TODO: is this mention belong here? it is used in one place so it can have its own thingy
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

fun UIText.withoutPreviewedLink(linkPreviews: List<MessageLinkPreview>): UIText {
    val dynamicText = this as? UIText.DynamicString ?: return this
    val preview = linkPreviews.firstOrNull() ?: return this
    val linkStart = preview.urlOffset
    val linkEnd = linkStart + preview.url.length
    if (linkStart < 0 || linkEnd > dynamicText.value.length) return this
    if (dynamicText.value.substring(linkStart, linkEnd) != preview.url) return this

    val whitespaceBeforeStart = generateSequence(linkStart - 1) { index -> index - 1 }
        .takeWhile { index -> index >= 0 && dynamicText.value[index].isWhitespace() }
        .lastOrNull()
        ?.plus(0)
        ?: linkStart
    val whitespaceAfterEnd = generateSequence(linkEnd) { index -> index + 1 }
        .takeWhile { index -> index < dynamicText.value.length && dynamicText.value[index].isWhitespace() }
        .lastOrNull()
        ?.plus(1)
        ?: linkEnd

    val hasContentBefore = dynamicText.value.substring(0, whitespaceBeforeStart).isNotEmpty()
    val hasContentAfter = dynamicText.value.substring(whitespaceAfterEnd).isNotEmpty()

    val removeStart = if (whitespaceBeforeStart < linkStart) whitespaceBeforeStart else linkStart
    val removeEnd = if (whitespaceAfterEnd > linkEnd) whitespaceAfterEnd else linkEnd
    val prefix = dynamicText.value.substring(0, removeStart)
    val suffix = dynamicText.value.substring(removeEnd)
    val replacement = if (hasContentBefore && hasContentAfter) " " else ""
    val removedLength = (removeEnd - removeStart) - replacement.length
    val updatedValue = buildString(dynamicText.value.length - removedLength) {
        append(prefix)
        append(replacement)
        append(suffix)
    }

    val adjustedMentions = dynamicText.mentions.mapNotNull { mention ->
        val mentionEnd = mention.start + mention.length
        when {
            mentionEnd <= removeStart -> mention
            mention.start >= removeEnd -> mention.copy(start = mention.start - removedLength)
            else -> null
        }
    }

    return UIText.DynamicString(
        value = updatedValue,
        mentions = adjustedMentions
    )
}

@Suppress("SpreadOperator")
fun UIText.resolveForTest(fakeStrings: Map<Int, String>): String = when (this) {
    is UIText.DynamicString -> this.value
    is UIText.StringResource -> {
        val raw = fakeStrings[this.resId]
            ?: error("Missing fake string for resId ${this.resId}")
        val resolvedArgs = formatArgs.map {
            when (it) {
                is UIText -> it.resolveForTest(fakeStrings)
                else -> it.toString()
            }
        }.toTypedArray()
        String.format(raw, *resolvedArgs)
    }

    else -> ""
}
