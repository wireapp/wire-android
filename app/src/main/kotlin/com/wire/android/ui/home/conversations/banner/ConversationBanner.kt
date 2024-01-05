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

package com.wire.android.ui.home.conversations.banner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.StatusLabel
import com.wire.android.util.ui.UIText

@Composable
fun ConversationBanner(bannerMessage: UIText?) {
    bannerMessage?.let {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.primary)
                .padding(vertical = dimensions().spacing6x, horizontal = dimensions().spacing16x),
            contentAlignment = Alignment.Center
        ) {
            StatusLabel(it.asString())
        }
    }
}

@Preview
@Composable
fun PreviewConversationBanner() {
    ConversationBanner(bannerMessage = UIText.DynamicString("Federated users, Externals, guests and services are present"))
}
