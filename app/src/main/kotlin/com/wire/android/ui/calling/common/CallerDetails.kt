package com.wire.android.ui.calling.common

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import com.wire.android.ui.common.MembershipQualifierLabel
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.hasLabel
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.call.ConversationType

@Composable
fun CallerDetails(
    conversationName: ConversationName?,
    isCameraOn: Boolean,
    avatarAssetId: ImageAsset.UserAvatarAsset?,
    conversationType: ConversationType,
    membership: Membership,
    isCallingLabel: String
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
            style = MaterialTheme.wireTypography.title01,
            modifier = Modifier.padding(top = dimensions().spacing24x)
        )
        Text(
            text = isCallingLabel,
            style = MaterialTheme.wireTypography.body01,
            modifier = Modifier.padding(top = dimensions().spacing8x)
        )
        if (membership.hasLabel()) {
            Spacer(Modifier.height(dimensions().spacing16x))
            MembershipQualifierLabel(membership)
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

@Preview
@Composable
fun CallerDetailsPreview() {
    CallerDetails(
        conversationName = ConversationName.Known("User"),
        isCameraOn = false,
        avatarAssetId = null,
        conversationType = ConversationType.OneOnOne,
        membership = Membership.Guest
    )
}
