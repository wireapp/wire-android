package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs

@Composable
fun BlockedUserComposerInput() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorsScheme().backgroundVariant)
            .padding(dimensions().spacing16x)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_conversation),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = "",
            modifier = Modifier
                .padding(start = dimensions().spacing8x)
                .size(dimensions().spacing12x)
        )
        Text(
            text = LocalContext.current.resources.stringWithStyledArgs(
                R.string.label_system_message_blocked_user,
                MaterialTheme.wireTypography.body01,
                MaterialTheme.wireTypography.body02,
                colorsScheme().secondaryText,
                colorsScheme().onBackground,
                stringResource(id = R.string.member_name_you_label_titlecase)
            ),
            style = MaterialTheme.wireTypography.body01,
            maxLines = 1,
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .padding(start = dimensions().spacing16x)
        )
    }
}
