package com.wire.android.ui

import com.wire.android.ui.home.conversations.SelfDeletionTimer
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.kalium.logic.data.message.Message
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SelfDeletionTimerTest {

    @Test
    fun givenTimeLeftIsAboveOneHour_whenGettingTheUpdateInterval_ThenIsEqualToMinutesLeftTillWholeHour() {
        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 23.hours + 30.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)

        val interval = (selfDeletionTimer as SelfDeletionTimer.Expirable).updateInterval()

        assert(interval == 30.minutes)
    }

    @Test
    fun givenTimeLeftIsEqualToWholeHour_whenGettingTheUpdateInterval_ThenIsEqualToOneMinute() {
        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 23.hours,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)

        val interval = (selfDeletionTimer as SelfDeletionTimer.Expirable).updateInterval()
        assert(interval == 1.minutes)
    }

    @Test
    fun givenTimeLeftIsEqualToOneHour_whenGettingTheUpdateInterval_ThenIsEqualToOneMinute() {
        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.hours,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)

        val interval = (selfDeletionTimer as SelfDeletionTimer.Expirable).updateInterval()
        assert(interval == 1.minutes)
    }

    @Test
    fun givenTimeLeftIsEqualToOneMinute_whenGettingTheUpdateInterval_ThenIsEqualToOneSeconds() {
        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)

        val interval = (selfDeletionTimer as SelfDeletionTimer.Expirable).updateInterval()
        assert(interval == 1.seconds)
    }

    @Test
    fun givenTimeLeftIsEqualToThirtySeconds_whenGettingTheUpdateInterval_ThenIsEqualToOneSeconds() {
        val selfDeletionTimer = SelfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 30.seconds,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimer.Expirable)

        val interval = (selfDeletionTimer as SelfDeletionTimer.Expirable).updateInterval()
        assert(interval == 1.seconds)
    }

}
