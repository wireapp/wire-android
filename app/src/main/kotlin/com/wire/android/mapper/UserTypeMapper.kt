package com.wire.android.mapper

import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.user.type.UserType
import javax.inject.Inject

class UserTypeMapper @Inject constructor() {

    fun toMembership(userType: UserType) = when (userType) {
        UserType.GUEST -> Membership.Guest
        UserType.FEDERATED -> Membership.Federated
        UserType.EXTERNAL -> Membership.External
        UserType.INTERNAL -> Membership.Internal
        UserType.NONE -> Membership.None
        UserType.SERVICE -> Membership.Service
        UserType.ADMIN -> Membership.Admin
        UserType.OWNER -> Membership.Owner
    }

}
