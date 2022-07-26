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
import androidx.compose.ui.unit.dp
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.hasLabel
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography


@Composable
fun MembershipQualifierLabel(membership: Membership, modifier: Modifier = Modifier) {
    if (membership.hasLabel()) {
        Box(
            modifier = modifier.border(
                width = MaterialTheme.wireDimensions.spacing1x,
                shape = RoundedCornerShape(MaterialTheme.wireDimensions.spacing4x),
                color = MaterialTheme.wireColorScheme.secondaryButtonSelectedOutline
            )
        ) {
            Text(
                text = stringResource(id = membership.stringResourceId),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.wireTypography.label03.copy(textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .wrapContentWidth()
                    .background(
                        color = MaterialTheme.wireColorScheme.secondaryButtonSelected,
                        shape = RoundedCornerShape(MaterialTheme.wireDimensions.corner4x)
                    )
                    .padding(horizontal = MaterialTheme.wireDimensions.spacing4x, vertical = MaterialTheme.wireDimensions.spacing2x)
            )
        }
    }
}

@Preview
@Composable
fun MembershipQualifierLabelPreview() {
    MembershipQualifierLabel(membership = Membership.Guest)
}
