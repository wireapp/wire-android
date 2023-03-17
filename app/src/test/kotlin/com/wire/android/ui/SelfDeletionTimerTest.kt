package com.wire.android.ui

import com.wire.android.ui.home.conversations.SelfDeletionTimer
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.kalium.logic.data.message.Message
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SelfDeletionTimerTest {
    
    @Test
    fun test() {
        val test = SelfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 60.seconds,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(test is SelfDeletionTimer.Expirable)

        val test1 = (test as SelfDeletionTimer.Expirable)

        val interval = test1.interval()



    }

}
