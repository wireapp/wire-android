package com.wire.android.ui.calling.common

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.calling.ConversationName
import com.wire.android.ui.common.SecurityClassificationBanner
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.hasLabel
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

@Composable
fun CallerDetails(
    conversationName: ConversationName?,
    isCameraOn: Boolean,
    avatarAssetId: ImageAsset.UserAvatarAsset?,
    conversationType: ConversationType,
    membership: Membership,
    callingLabel: String,
    securityClassificationType: SecurityClassificationType
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current
        IconButton(
            modifier = Modifier
                .padding(top = dimensions().spacing16x, start = dimensions().spacing6x)
                .align(Alignment.Start)
                .rotate(180f),
            onClick = {
                Toast.makeText(context, "Not implemented yet =)", Toast.LENGTH_SHORT).show()
            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_collapse),
                contentDescription = stringResource(id = R.string.calling_minimize_view),
            )
        }
        Text(
            text = when (conversationName) {
                is ConversationName.Known -> conversationName.name
                is ConversationName.Unknown -> stringResource(id = conversationName.resourceId)
                else -> ""
            },
            color = colorsScheme().onBackground,
            style = MaterialTheme.wireTypography.title01,
            modifier = Modifier.padding(top = dimensions().spacing24x)
        )
        Text(
            text = callingLabel,
            color = colorsScheme().onBackground,
            style = MaterialTheme.wireTypography.body01,
            modifier = Modifier.padding(top = dimensions().spacing8x)
        )
        if (membership.hasLabel()) {
            VerticalSpace.x16()
            MembershipQualifierLabel(membership)
        }
        if (securityClassificationType != SecurityClassificationType.NONE) {
            VerticalSpace.x8()
            SecurityClassificationBanner(securityClassificationType = securityClassificationType)
        }

        if (!isCameraOn && conversationType == ConversationType.OneOnOne) {
            UserProfileAvatar(
                avatarData = UserAvatarData(avatarAssetId),
                size = dimensions().initiatingCallUserAvatarSize,
                modifier = Modifier.padding(top = dimensions().spacing16x)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCallerDetails() {
    CallerDetails(
        conversationName = ConversationName.Known("User"),
        isCameraOn = false,
        avatarAssetId = null,
        conversationType = ConversationType.OneOnOne,
        membership = Membership.Guest,
        callingLabel = String.EMPTY,
        securityClassificationType = SecurityClassificationType.CLASSIFIED
    )
}
