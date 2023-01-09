package com.wire.android.workmanager

import com.wire.android.R
import com.wire.kalium.logic.sync.ForegroundNotificationDetailsProvider

object WireForegroundNotificationDetailsProvider: ForegroundNotificationDetailsProvider {
    override fun getSmallIconResId(): Int = R.drawable.notification_icon_small
}
