package com.wire.android.ui

import com.wire.android.ui.home.conversations.SelfDeletionTimer
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SelfDeletionTimerTest {


    @Test
    fun test() {
        val test = SelfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 10.seconds,
                selfDeletionStatus =
            )
        )
    }

}
