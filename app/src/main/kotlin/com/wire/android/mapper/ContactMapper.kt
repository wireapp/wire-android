package com.wire.android.mapper

import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.userprofile.common.UsernameMapper.toUserLabel
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.OtherUser
import javax.inject.Inject

class ContactMapper
@Inject constructor(
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader
) {

    fun fromOtherUser(otherUser: OtherUser): Contact {
        with(otherUser) {
            return Contact(
                id = id.value,
                domain = id.domain,
                name = name.orEmpty(),
                label = toUserLabel(otherUser),
                avatarData = UserAvatarData(
                    asset = previewPicture?.let { ImageAsset.UserAvatarAsset(wireSessionImageLoader, it) },
                    connectionState = connectionStatus
                ),
                membership = userTypeMapper.toMembership(userType),
                connectionState = otherUser.connectionStatus
            )
        }
    }
}
