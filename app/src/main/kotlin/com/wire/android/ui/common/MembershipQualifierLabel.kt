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

package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.hasLabel
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun MembershipQualifierLabel(membership: Membership, modifier: Modifier = Modifier) {
    if (membership.hasLabel()) {
        Box(
            modifier = modifier.border(
                width = MaterialTheme.wireDimensions.spacing1x,
                shape = RoundedCornerShape(MaterialTheme.wireDimensions.spacing4x),
                color = colorsScheme().secondaryButtonSelectedOutline
            )
        ) {
            Text(
                text = stringResource(id = membership.stringResourceId),
                color = colorsScheme().onPrimaryVariant,
                style = MaterialTheme.wireTypography.label03.copy(textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .wrapContentWidth()
                    .background(
                        color = colorsScheme().primaryVariant,
                        shape = RoundedCornerShape(MaterialTheme.wireDimensions.corner4x)
                    )
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.spacing4x,
                        vertical = MaterialTheme.wireDimensions.spacing2x
                    )
            )
        }
    }
}

@Preview
@Composable
fun PreviewMembershipQualifierLabel() {
    MembershipQualifierLabel(membership = Membership.Guest)
}
