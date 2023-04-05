/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.SecurityClassificationBanner
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

@Composable
fun MessageComposerClassifiedBanner(
    securityClassificationType: SecurityClassificationType,
    paddingValues: PaddingValues = PaddingValues()
) {
    val isClassifiedConversation = securityClassificationType != SecurityClassificationType.NONE
    if (isClassifiedConversation) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colorsScheme().backgroundVariant)
                .padding(paddingValues)
        ) {
            VerticalSpace.x8()
            SecurityClassificationBanner(securityClassificationType = securityClassificationType)
        }
    }
}
