package com.wire.android.ui.home.conversations

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.kalium.logic.data.message.Message
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberSelfDeletionTimer(expirationStatus: ExpirationStatus): SelfDeletionTimer {
    return remember { SelfDeletionTimer.fromExpirationData(expirationStatus) }
}

sealed class SelfDeletionTimer {
    companion object {
        fun fromExpirationData(expirationStatus: ExpirationStatus?): SelfDeletionTimer {
            return if (expirationStatus is ExpirationStatus.Expirable) {
                with(expirationStatus) {
                    val timeLeft = if (selfDeletionStatus is Message.ExpirationData.SelfDeletionStatus.Started) {
                        val timeElapsedSinceSelfDeletionStartDate =
                            Clock.System.now() - selfDeletionStatus.selfDeletionStartDate

                        // time left for deletion it can be a negative value if the time difference between the self deletion start date and
                        // now is greater then expire after millis, we normalize it to 0 seconds
                        val timeLeft = expireAfter - timeElapsedSinceSelfDeletionStartDate

                        if (timeLeft.isNegative()) {
                            Duration.ZERO
                        } else {
                            timeLeft
                        }
                    } else {
                        expireAfter
                    }

                    Expirable(timeLeft, expireAfter)
                }
            } else NotExpirable
        }
    }

    class Expirable(timeLeft: Duration, private val expireAfter: Duration) : SelfDeletionTimer() {
        companion object {
            private const val DAYS_IN_A_WEEK = 7
            private const val FOUR_WEEK_DAYS = DAYS_IN_A_WEEK * 4
            private const val THREE_WEEK_DAYS = DAYS_IN_A_WEEK * 3
            private const val TWO_WEEK_DAYS = DAYS_IN_A_WEEK * 2
            private const val ONE_WEEK_DAYS = DAYS_IN_A_WEEK * 1

            private const val TIME_LEFT_RATIO_BOUNDARY_FOR_1_ALPHA = 0.75
        }

        var timeLeft by mutableStateOf(timeLeft)

        fun timeLeftFormatted(): String {
            val timeLeftLabel = when {
                // weeks
                timeLeft.inWholeDays >= FOUR_WEEK_DAYS -> "4 weeks"
                timeLeft.inWholeDays in THREE_WEEK_DAYS until FOUR_WEEK_DAYS -> "3 weeks left"
                timeLeft.inWholeDays in TWO_WEEK_DAYS until THREE_WEEK_DAYS -> "2 weeks left"
                timeLeft.inWholeDays in ONE_WEEK_DAYS until TWO_WEEK_DAYS -> "1 week left"
                // days
                timeLeft.inWholeDays in 1..6 -> "${timeLeft.inWholeDays} days left"
                // hours
                timeLeft.inWholeHours in 1..23 -> "${timeLeft.inWholeHours} hours left"
                // minutes
                timeLeft.inWholeMinutes in 1..59 -> "${timeLeft.inWholeMinutes} minutes left"
                // seconds
                timeLeft.inWholeSeconds < 60 -> "${timeLeft.inWholeSeconds} seconds left "

                else -> throw IllegalStateException("Not possible state for a time left label")
            }

            return timeLeftLabel
        }

        fun interval(): Duration {
            val interval = when {
                timeLeft.inWholeMinutes > 59 -> 1.hours
                timeLeft.inWholeMinutes in 2..59 -> 1.minutes
                timeLeft.inWholeSeconds <= 60 && timeLeft.inWholeMinutes < 2 -> 1.seconds
                else -> throw IllegalStateException("Not possible state for interval")
            }

            return interval
        }

        fun decreaseTimeLeft(interval: Duration) {
            if (timeLeft.inWholeSeconds != 0L) timeLeft -= interval
        }

        fun alphaBackgroundColor(): Float {
            val totalTimeLeftRatio = timeLeft / expireAfter

            return if (totalTimeLeftRatio >= TIME_LEFT_RATIO_BOUNDARY_FOR_1_ALPHA) {
                0F
            } else {
                1F
            }
        }

    }

    object NotExpirable : SelfDeletionTimer()

}
