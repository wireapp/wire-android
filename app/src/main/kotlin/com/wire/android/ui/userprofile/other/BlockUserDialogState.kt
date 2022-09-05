package com.wire.android.ui.userprofile.other

import com.wire.kalium.logic.data.user.UserId

data class BlockUserDialogState(val userName: String, val userId: UserId)
data class UnblockUserDialogState(val username: String, val userId: UserId)
