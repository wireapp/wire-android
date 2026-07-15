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

package com.wire.android.ui.home.conversations.messages.item

import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.linkpreview.MessageLinkPreview

internal fun MessageBody.shouldHideStandalonePreviewedUrl(preview: MessageLinkPreview): Boolean {
    val text = (message as? UIText.DynamicString)?.value

    return text
        ?.let { dynamicText ->
            preview.urlRangeWithin(dynamicText)?.let { urlRange ->
                val textBeforeUrl = dynamicText.substring(0, urlRange.first)
                val textAfterUrl = dynamicText.substring(urlRange.last + 1)

                textBeforeUrl.isBlank() && textAfterUrl.isBlank()
            }
        }
        ?: false
}

@Suppress("ComplexCondition")
private fun MessageLinkPreview.urlRangeWithin(text: String): IntRange? {
    val urlStart = urlOffset

    if (url.isEmpty() || urlStart < 0 || url.length > text.length || urlStart > text.length - url.length) {
        return null
    }

    val urlEndExclusive = urlStart + url.length

    return if (text.substring(urlStart, urlEndExclusive) == url) {
        urlStart until urlEndExclusive
    } else {
        null
    }
}
