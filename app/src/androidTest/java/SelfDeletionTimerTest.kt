package com.wire.android.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.ui.home.conversations.SelfDeletionTimer
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.kalium.logic.data.message.Message
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SelfDeletionTimerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenTimeLeftIsAboveOneHour_whenGettingTheUpdateInterval_ThenIsEqualToMinutesLeftTillWholeHour() {


        val selfDeletionTimer = SelfDeletionTimer(context).fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 23.hours + 30.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )

        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)

        val interval = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).updateInterval()

        assert(interval == 30.minutes)
    }

//    @Test
//    fun givenTimeLeftIsEqualToWholeHour_whenGettingTheUpdateInterval_ThenIsEqualToOneMinute() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 23.hours,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val interval = (selfDeletionTimer as SelfDeletionTimer.Expirable).updateInterval()
//        assert(interval == 1.minutes)
//    }
//
//    @Test
//    fun givenTimeLeftIsEqualToOneHour_whenGettingTheUpdateInterval_ThenIsEqualToOneMinute() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 1.hours,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val interval = (selfDeletionTimer as SelfDeletionTimer.Expirable).updateInterval()
//        assert(interval == 1.minutes)
//    }
//
//    @Test
//    fun givenTimeLeftIsEqualToOneMinute_whenGettingTheUpdateInterval_ThenIsEqualToOneSeconds() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 1.minutes,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val interval = (selfDeletionTimer as SelfDeletionTimer.Expirable).updateInterval()
//        assert(interval == 1.seconds)
//    }
//
//    @Test
//    fun givenTimeLeftIsEqualToThirtySeconds_whenGettingTheUpdateInterval_ThenIsEqualToOneSeconds() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 30.seconds,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val interval = (selfDeletionTimer as SelfDeletionTimer.Expirable).updateInterval()
//        assert(interval == 1.seconds)
//    }
//
//
//    @Test
//    fun test() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 50.days,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "4 weeks")
//    }
//
//    @Test
//    fun test1() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 21.days,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "3 weeks")
//    }
//
//
//    @Test
//    fun test3() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 27.days,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "3 weeks")
//    }
//
//    @Test
//    fun test4() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 14.days,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "2 weeks")
//    }
//
//
//    @Test
//    fun test5() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 20.days,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "2 weeks")
//    }
//
//
//    @Test
//    fun test6() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 7.days,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "1 week")
//    }
//
//
//    @Test
//    fun test7() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 13.days,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "1 week")
//    }
//
//
//    @Test
//    fun test8() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 1.days,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "1 days left")
//    }
//
//
//    @Test
//    fun test9() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 6.days,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "6 days left")
//    }
//
//    @Test
//    fun test10() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 1.days,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "1 days left")
//    }
//
//
//    @Test
//    fun test11() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 24.hours,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "1 days left")
//    }
//
//    @Test
//    fun test12() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 60.minutes,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "1 hours left")
//    }
//
//    @Test
//    fun test13() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 1.hours,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "1 hours left")
//    }
//
//    @Test
//    fun test14() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 1.minutes,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "1 minutes left")
//    }
//
//    @Test
//    fun test15() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 59.minutes,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "59 minutes left")
//    }
//
//    @Test
//    fun test16() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 60.seconds,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "1 minutes left")
//    }
//
//    @Test
//    fun test17() {
//        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
//            ExpirationStatus.Expirable(
//                expireAfter = 59.seconds,
//                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
//            )
//        )
//        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)
//
//        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.Expirable).timeLeftFormatted()
//
//        assert(timeLeftLabel == "59 seconds left")
//    }

}
