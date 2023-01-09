package com.wire.android.ui.common.dialogs

import androidx.annotation.StringRes
import com.wire.kalium.logic.data.user.UserId

data class BlockUserDialogState(val userName: String, val userId: UserId)
data class UnblockUserDialogState(val userName: String, val userId: UserId)
data class FeatureDisabledWithProxyDialogState(@StringRes val description: Int, val teamUrl: String = "")
object CancelLoginDialogState

