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
package com.wire.android.ui.home.conversations.sendmessage

import com.wire.kalium.logic.data.message.mention.MessageMention

internal data class ComposerLinkPreviewTarget(
    val url: String,
    val position: Int,
)

internal object ComposerLinkPreviewTargetDetector {
    private val webUrl = Regex(
        "(?:(?:https?|ftp)://(?:(?:[a-z0-9](?:[a-z0-9\\-]*[a-z0-9])?\\.)+" +
            "(?:com|org|net|edu|gov|mil|int|mobi|name|aero|asia|biz|cat|coop|info|jobs|museum|tel|travel|xxx|[a-z]{2})|" +
            "(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9]?[0-9])\\.){3}" +
            "(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9]?[0-9])|" +
            "(?:(?:[0-9a-f]{0,4}:){2,7}[0-9a-f]{0,4}))(?::[0-9]+)?" +
            "(?:/[a-z0-9\\-._~:/?#\\[\\]@!\$&'()*+,;=]*)?)|" +
            "(?:www\\.(?:[a-z0-9](?:[a-z0-9\\-]*[a-z0-9])?\\.)+" +
            "(?:com|org|net|edu|gov|mil|int|mobi|name|aero|asia|biz|cat|coop|info|jobs|museum|tel|travel|xxx|[a-z]{2})|" +
            "(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9]?[0-9])\\.){3}" +
            "(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9]?[0-9])|" +
            "(?:(?:[0-9a-f]{0,4}:){2,7}[0-9a-f]{0,4}))(?::[0-9]+)?" +
            "(?:/[a-z0-9\\-._~:/?#\\[\\]@!\$&'()*+,;=]*)?",
        RegexOption.IGNORE_CASE
    )

    fun detect(
        text: String,
        mentions: List<MessageMention> = emptyList(),
    ): ComposerLinkPreviewTarget? {
        val excludedRanges = buildExcludedRanges(text, mentions)
        return webUrl.findAll(text)
            .map { ComposerLinkPreviewTarget(it.value, it.range.first) }
            .firstOrNull { candidate ->
                val candidateRange = candidate.position until (candidate.position + candidate.url.length)
                excludedRanges.none { excluded ->
                    candidateRange.first < excluded.last + 1 && excluded.first < candidateRange.last + 1
                }
            }
    }

    private fun buildExcludedRanges(
        text: String,
        mentions: List<MessageMention>,
    ): List<IntRange> {
        val ranges = mentions.map { it.start until (it.start + it.length) }.toMutableList()
        Regex("""\[.+?\]\((.+?)\)""")
            .findAll(text)
            .forEach { ranges.add(it.range) }
        return ranges
    }
}
