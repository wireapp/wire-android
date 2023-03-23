package com.wire.android.ui.home.conversations

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.wire.android.R
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.kalium.logic.data.message.Message
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberSelfDeletionTimer(expirationStatus: ExpirationStatus): SelfDeletionTimer.SelfDeletionTimerState {
    val context = LocalContext.current

    return remember { SelfDeletionTimer(context).fromExpirationStatus(expirationStatus) }
}

class SelfDeletionTimer(private val context: Context) {

    fun fromExpirationStatus(expirationStatus: ExpirationStatus): SelfDeletionTimerState {
        return if (expirationStatus is ExpirationStatus.Expirable) {
            with(expirationStatus) {
                val timeLeft = if (selfDeletionStatus is Message.ExpirationData.SelfDeletionStatus.Started) {
                    val timeElapsedSinceSelfDeletionStartDate = Clock.System.now() - selfDeletionStatus.selfDeletionStartDate

                    // time left for deletion it can be a negative value if the time difference between the self deletion start date and
                    // now is greater then expire after millis, we normalize it to 0 seconds
                    val timeLeft = expireAfter - timeElapsedSinceSelfDeletionStartDate

                    if (timeLeft.isNegative()) {
                        ZERO
                    } else {
                        timeLeft
                    }
                } else {
                    expireAfter
                }

                SelfDeletionTimerState.Expirable(context.resources, timeLeft, expireAfter)
            }
        } else SelfDeletionTimerState.NotExpirable
    }

    sealed class SelfDeletionTimerState {

        class Expirable(
            private val resources: Resources,
            timeLeft: Duration,
            private val expireAfter: Duration
        ) :
            SelfDeletionTimerState() {
            companion object {
                private const val TIME_LEFT_RATIO_BOUNDARY_FOR_ALMOST_TIME_LEFT_ELAPSED_ALPHA = 0.75
                private const val PAST_RATIO_BOUNDARY_FOR_ALMOST_TIME_LEFT_ALPHA_VALUE = 0F
                private const val BEFORE_RATIO_BOUNDARY_FOR_ALMOST_TIME_LEFT_ALPHA_VALUE = 1F
            }

            var timeLeft by mutableStateOf(timeLeft)

            fun timeLeftFormatted(): String {
                val timeLeftLabel = when {
                    // weeks
                    timeLeft >= 28.days -> {
                        resources.getQuantityString(
                            R.plurals.weeks_left,
                            4,
                            4
                        )
                    }

                    timeLeft >= 21.days && timeLeft < 28.days ->
                        resources.getQuantityString(
                            R.plurals.weeks_left,
                            3,
                            3
                        )

                    timeLeft >= 14.days && timeLeft < 21.days ->
                        resources.getQuantityString(
                            R.plurals.weeks_left,
                            2,
                            2
                        )

                    timeLeft >= 7.days && timeLeft < 14.days ->
                        resources.getQuantityString(
                            R.plurals.weeks_left,
                            1,
                            1
                        )
                    // days
                    timeLeft >= 1.days && timeLeft < 7.days ->
                        resources.getQuantityString(
                            R.plurals.days_left,
                            timeLeft.inWholeDays.toInt(),
                            timeLeft.inWholeDays.toInt()
                        )
                    // hours
                    timeLeft >= 1.hours && timeLeft < 24.hours ->
                        resources.getQuantityString(
                            R.plurals.hours_left,
                            timeLeft.inWholeHours.toInt(),
                            timeLeft.inWholeHours.toInt()
                        )
                    // minutes
                    timeLeft >= 1.minutes && timeLeft < 60.minutes ->
                        resources.getQuantityString(
                            R.plurals.minutes_left,
                            timeLeft.inWholeMinutes.toInt(),
                            timeLeft.inWholeMinutes.toInt()
                        )
                    // seconds
                    timeLeft < 60.seconds ->
                        resources.getQuantityString(
                            R.plurals.seconds_left,
                            timeLeft.inWholeSeconds.toInt(),
                            timeLeft.inWholeSeconds.toInt()
                        )

                    else -> throw IllegalStateException("Not possible state for a time left label")
                }

                return timeLeftLabel
            }

            // we returns minute in case we fit exactly 60 minutes into a day or 60 minutes into a hour,
            // in that case we would return 0 as interval, which would mean that the timer would never update
            // any better ideas for this ?
            fun updateInterval(): Duration {
                val timeLeftUpdateInterval = when {
                    timeLeft > 24.hours -> {
                        val timeLeftTillWholeDay = ((timeLeft).inWholeMinutes % (1.days.inWholeMinutes)).minutes
                        if (timeLeftTillWholeDay == ZERO) {
                            1.minutes
                        } else {
                            timeLeftTillWholeDay
                        }
                    }

                    timeLeft <= 24.hours && timeLeft > 1.hours -> {
                        val timeLeftTillWholeHour = (timeLeft.inWholeMinutes % 1.hours.inWholeMinutes).minutes
                        if (timeLeftTillWholeHour == ZERO) {
                            1.minutes
                        } else {
                            timeLeftTillWholeHour
                        }
                    }

                    timeLeft <= 1.hours && timeLeft > 1.minutes -> {
                        1.minutes
                    }

                    timeLeft <= 1.minutes -> {
                        1.seconds
                    }

                    else -> throw IllegalStateException("Not possible state for the interval")
                }

                return timeLeftUpdateInterval
            }

            fun decreaseTimeLeft(interval: Duration) {
                if (timeLeft.inWholeSeconds != 0L) timeLeft -= interval
            }

            fun alphaBackgroundColor(): Float {
                val totalTimeLeftRatio = timeLeft / expireAfter

                return if (totalTimeLeftRatio >= TIME_LEFT_RATIO_BOUNDARY_FOR_ALMOST_TIME_LEFT_ELAPSED_ALPHA) {
                    PAST_RATIO_BOUNDARY_FOR_ALMOST_TIME_LEFT_ALPHA_VALUE
                } else {
                    BEFORE_RATIO_BOUNDARY_FOR_ALMOST_TIME_LEFT_ALPHA_VALUE
                }
            }

        }

        object NotExpirable : SelfDeletionTimerState()

    }
}
