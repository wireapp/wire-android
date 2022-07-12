package com.wire.android.ui.common

import androidx.compose.foundation.background
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
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.hasLabel
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography


@Composable
fun MembershipQualifierLabel(
    membership: Membership,
) {
    if (membership.hasLabel()) {
        Text(
            text = stringResource(id = membership.stringResourceId),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.wireTypography.label03.copy(textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .wrapContentWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(MaterialTheme.wireDimensions.corner4x)
                )
                .padding(horizontal = MaterialTheme.wireDimensions.spacing4x, vertical = MaterialTheme.wireDimensions.spacing2x)
        )
    }
}
