package com.wire.android

import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.ui.home.conversations.SelfDeletionTimer
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.kalium.logic.data.message.Message
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SelfDeletionTimerTest {

    private val selfDeletionTimer = SelfDeletionTimer(
        context = InstrumentationRegistry.getInstrumentation().targetContext
    )

    @Test
    fun givenTimeLeftIsAboveOneHour_whenGettingTheUpdateInterval_ThenIsEqualToMinutesLeftTillWholeHour() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 23.hours + 30.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val interval = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).updateInterval()
        assert(interval == 30.minutes)
    }

    @Test
    fun givenTimeLeftIsEqualToWholeHour_whenGettingTheUpdateInterval_ThenIsEqualToOneMinute() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 23.hours,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val interval = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).updateInterval()
        assert(interval == 1.minutes)
    }

    @Test
    fun givenTimeLeftIsEqualToOneHour_whenGettingTheUpdateInterval_ThenIsEqualToOneMinute() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.hours,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val interval = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).updateInterval()
        assert(interval == 1.minutes)
    }

    @Test
    fun givenTimeLeftIsEqualToOneMinute_whenGettingTheUpdateInterval_ThenIsEqualToOneSeconds() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val interval = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).updateInterval()
        assert(interval == 1.seconds)
    }

    @Test
    fun givenTimeLeftIsEqualToThirtySeconds_whenGettingTheUpdateInterval_ThenIsEqualToOneSeconds() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 30.seconds,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val interval = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).updateInterval()
        assert(interval == 1.seconds)
    }

    @Test
    fun givenTimeLeftIsEqualToFiftyDays_whenGettingThTimeLeftFormatted_ThenIsEqualToFourWeeksLeft() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 50.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "4 weeks left")
    }

    @Test
    fun givenTimeLeftIsEqualToTwentyOneDays_whenGettingThTimeLeftFormatted_ThenIsEqualToThreeWeeksLeft() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 21.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "3 weeks left")
    }

    @Test
    fun givenTimeLeftIsEqualToTwentySevenDays_whenGettingThTimeLeftFormatted_ThenIsEqualToFourWeeksLeft() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 27.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "4 weeks left")
    }

    @Test
    fun givenTimeLeftIsEqualToFourTeenDays_whenGettingThTimeLeftFormatted_ThenIsEqualToTwoWeeksLeft() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 14.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "2 weeks left")
    }

    @Test
    fun givenTimeLeftIsEqualToTwentyDays_whenGettingThTimeLeftFormatted_ThenIsEqualToTwoWeeksLeft() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 20.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "2 weeks left")
    }

    @Test
    fun givenTimeLeftIsEqualToSevenDays_whenGettingThTimeLeftFormatted_ThenIsEqualToOneWeekLeft() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 7.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "1 week left")
    }

    @Test
    fun givenTimeLeftIsEqualToThirteenDays_whenGettingThTimeLeftFormatted_ThenIsEqualToOneWeekLeft() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 13.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "2 weeks left")
    }

    @Test
    fun givenTimeLeftIsEqualToOneDay_whenGettingThTimeLeftFormatted_ThenIsEqualToOneDayLeft() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "1 day left")
    }

    @Test
    fun givenTimeLeftIsEqualToSixDays_whenGettingThTimeLeftFormatted_ThenIsEqualToSixDaysLeft() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 6.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "6 days left")
    }

    @Test
    fun givenTimeLeftIsEqualToTwentyFourHours_whenGettingThTimeLeftFormatted_ThenIsEqualToOneDayLeft() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 24.hours,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "1 day left")
    }

    @Test
    fun givenTimeLeftIsEqualToSixtyMinutes_whenGettingThTimeLeftFormatted_ThenIsEqualToOneHourLeft() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 60.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "1 hour left")
    }

    @Test
    fun givenTimeLeftIsEqualToOneMinute_whenGettingThTimeLeftFormatted_ThenIsEqualToOneMinuteLeft() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )

        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "1 minute left")
    }

    @Test
    fun givenTimeLeftIsEqualToOFiftyNineMinutes_whenGettingThTimeLeftFormatted_ThenIsEqualToFiftyNineMinutes() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 59.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "59 minutes left")
    }

    @Test
    fun givenTimeLeftIsEqualToSixtySeconds_whenGettingThTimeLeftFormatted_ThenIsEqualToOneMinute() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 60.seconds,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimer.SelfDeletionTimerState.Expirable).timeLeftFormatted()
        assert(timeLeftLabel == "1 minute left")
    }

}
