/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.conversations

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.wire.android.R
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.kalium.logic.data.message.Message
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberSelfDeletionTimer(expirationStatus: ExpirationStatus): SelfDeletionTimerHelper.SelfDeletionTimerState {
    val context = LocalContext.current

    return remember(
        (expirationStatus as? ExpirationStatus.Expirable)?.selfDeletionStatus ?: true
    ) { SelfDeletionTimerHelper(context).fromExpirationStatus(expirationStatus) }
}

class SelfDeletionTimerHelper(private val context: Context) {

    fun fromExpirationStatus(expirationStatus: ExpirationStatus): SelfDeletionTimerState {
        return if (expirationStatus is ExpirationStatus.Expirable) {
            with(expirationStatus) {
                val timeLeft = calculateTimeLeft(selfDeletionStatus, expireAfter)
                SelfDeletionTimerState.Expirable(
                    context.resources,
                    timeLeft,
                    expireAfter,
                    selfDeletionStatus is Message.ExpirationData.SelfDeletionStatus.Started
                )
            }
        } else {
            SelfDeletionTimerState.NotExpirable
        }
    }

    private fun calculateTimeLeft(
        selfDeletionStatus: Message.ExpirationData.SelfDeletionStatus?,
        expireAfter: Duration
    ): Duration {
        return if (selfDeletionStatus is Message.ExpirationData.SelfDeletionStatus.Started) {
            val timeElapsedSinceSelfDeletionStartDate = Clock.System.now() - selfDeletionStatus.selfDeletionStartDate
            val timeLeft = expireAfter - timeElapsedSinceSelfDeletionStartDate

            /**
             * time left for deletion, can be a negative value if the time difference between the self deletion start date and
             * Clock.System.now() is greater then [expireAfter], we normalize it to 0 seconds
             */
            if (timeLeft.isNegative()) {
                ZERO
            } else {
                timeLeft
            }
        } else {
            expireAfter
        }
    }

    sealed class SelfDeletionTimerState {

        class Expirable(
            private val resources: Resources,
            timeLeft: Duration,
            private val expireAfter: Duration,
            val timerStarted: Boolean
        ) : SelfDeletionTimerState() {
            companion object {
                /**
                 * high end boundary ratio between elapsed time which is equal to [expireAfter] - [timeLeft] and [expireAfter]
                 * on which the proportional background alpha color changes
                 */
                private const val HIGH_END_TIME_ELAPSED_RATIO_BOUNDARY_FOR_PROPORTIONAL_ALPHA_CHANGE = 0.75

                /**
                 * low end boundary ratio between elapsed time which is equal to [expireAfter] - [timeLeft] and [expireAfter]
                 * on which the proportional background alpha color changes
                 */
                private const val LOW_END_TIME_ELAPSED_RATIO_BOUNDARY_FOR_PROPORTIONAL_ALPHA_CHANGE = 0.50
                private const val QUARTER_RATIO_VALUE = 0.25
                private const val HALF_RATIO_VALUE = 0.50
                private const val INVISIBLE_BACKGROUND_COLOR_ALPHA_VALUE = 0F
                private const val OPAQUE_BACKGROUND_COLOR_ALPHA_VALUE = 1F
            }

            var timeLeft by mutableStateOf(timeLeft)

            @Suppress("MagicNumber", "ComplexMethod")
            fun timeLeftFormatted(): String = when {
                timeLeft > 28.days ->
                    resources.getQuantityString(
                        R.plurals.weeks_left,
                        4,
                        4
                    )
                // 4 weeks
                timeLeft >= 27.days && timeLeft <= 28.days ->
                    resources.getQuantityString(
                        R.plurals.weeks_left,
                        4,
                        4
                    )
                // days below 4 weeks
                timeLeft <= 27.days && timeLeft > 7.days ->
                    resources.getQuantityString(
                        R.plurals.days_left,
                        timeLeft.inWholeDays.toInt(),
                        timeLeft.inWholeDays.toInt()
                    )
                // one week
                timeLeft >= 6.days && timeLeft <= 7.days ->
                    resources.getQuantityString(
                        R.plurals.weeks_left,
                        1,
                        1
                    )
                // days below 1 week
                timeLeft < 7.days && timeLeft >= 1.days ->
                    resources.getQuantityString(
                        R.plurals.days_left,
                        timeLeft.inWholeDays.toInt(),
                        timeLeft.inWholeDays.toInt()
                    )
                // hours below one day
                timeLeft >= 1.hours && timeLeft < 24.hours ->
                    resources.getQuantityString(
                        R.plurals.hours_left,
                        timeLeft.inWholeHours.toInt(),
                        timeLeft.inWholeHours.toInt()
                    )
                // minutes below hour
                timeLeft >= 1.minutes && timeLeft < 60.minutes ->
                    resources.getQuantityString(
                        R.plurals.minutes_left,
                        timeLeft.inWholeMinutes.toInt(),
                        timeLeft.inWholeMinutes.toInt()
                    )
                // seconds below minute
                timeLeft < 60.seconds ->
                    resources.getQuantityString(
                        R.plurals.seconds_left,
                        timeLeft.inWholeSeconds.toInt(),
                        timeLeft.inWholeSeconds.toInt()
                    )

                else -> throw IllegalStateException("Not possible state for a time left label")
            }

            /**
             * Calculates when the next update should be done based on the [timeLeft]
             * For example, it's unnecessary to update on each second when the timer
             * is still marking 4 weeks left.
             * However, when there's less than a minute left, the timer should be
             * updated every second.
             * @return how long until the next timer update.
             */
            fun updateInterval(): Duration {
                val timeLeftUpdateInterval = when {
                    timeLeft > 24.hours -> {
                        val timeLeftTillWholeDay = (timeLeft.inWholeMinutes % 1.days.inWholeMinutes).minutes
                        if (timeLeftTillWholeDay == ZERO) {
                            1.days
                        } else {
                            timeLeftTillWholeDay
                        }
                    }

                    timeLeft <= 24.hours && timeLeft > 1.hours -> {
                        val timeLeftTillWholeHour = (timeLeft.inWholeSeconds % 1.hours.inWholeSeconds).seconds
                        if (timeLeftTillWholeHour == ZERO) {
                            1.hours
                        } else {
                            timeLeftTillWholeHour
                        }
                    }

                    timeLeft <= 1.hours && timeLeft > 1.minutes -> {
                        val timeLeftTillWholeMinute = (timeLeft.inWholeSeconds % 1.minutes.inWholeSeconds).seconds
                        if (timeLeftTillWholeMinute == ZERO) {
                            1.minutes
                        } else {
                            timeLeftTillWholeMinute
                        }
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

            /**
             * if the time elapsed ratio is between 0.50 and 0.75
             * we want to change the value proportionally to how much
             * time is left between [LOW_END_TIME_ELAPSED_RATIO_BOUNDARY_FOR_PROPORTIONAL_ALPHA_CHANGE]
             * and [HIGH_END_TIME_ELAPSED_RATIO_BOUNDARY_FOR_PROPORTIONAL_ALPHA_CHANGE], we doing that by dividing
             * how much time is elapsed since half of the total expire after time by
             * the "time slice" that fits between 0.5 and 0.75
             * for example. [expireAfter] = 10 sec, timeElapsed = 6 sec
             * quarterTimeLeftSlice = 2.5 sec, halfTimeSlice = 5 sec
             * durationInBetweenHalfTimeAndQuarterSlice = 5 sec - 2.5 sec = 2.5 sec
             * timeElapsedFromHalfTimeSlice = 6 sec - 5 sec = 1 sec
             * alpha value is equal to the ratio = 1 / 2.5 = 0.4
             */
            fun alphaBackgroundColor(): Float {
                return if (timeLeft > ZERO) {
                    val timeElapsed = expireAfter - timeLeft
                    val timeElapsedRatio = timeElapsed / expireAfter

                    return if (timeElapsedRatio < LOW_END_TIME_ELAPSED_RATIO_BOUNDARY_FOR_PROPORTIONAL_ALPHA_CHANGE) {
                        INVISIBLE_BACKGROUND_COLOR_ALPHA_VALUE
                    } else if (LOW_END_TIME_ELAPSED_RATIO_BOUNDARY_FOR_PROPORTIONAL_ALPHA_CHANGE <= timeElapsedRatio
                        && timeElapsedRatio <= HIGH_END_TIME_ELAPSED_RATIO_BOUNDARY_FOR_PROPORTIONAL_ALPHA_CHANGE
                    ) {
                        val halfTimeSlice = expireAfter.times(HALF_RATIO_VALUE)
                        val quarterTimeLeftSlice = expireAfter.times(QUARTER_RATIO_VALUE)

                        val durationInBetweenHalfTimeAndQuarterSlice = halfTimeSlice - quarterTimeLeftSlice
                        val timeElapsedFromHalfTimeSlice = timeElapsed - halfTimeSlice

                        (timeElapsedFromHalfTimeSlice / durationInBetweenHalfTimeAndQuarterSlice).toFloat()
                    } else {
                        OPAQUE_BACKGROUND_COLOR_ALPHA_VALUE
                    }
                } else {
                    OPAQUE_BACKGROUND_COLOR_ALPHA_VALUE
                }
            }
        }

        object NotExpirable : SelfDeletionTimerState()
    }
}

@Composable
fun startDeletionTimer(
    message: UIMessage,
    expirableTimer: SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable,
    onStartMessageSelfDeletion: (UIMessage) -> Unit
) {
    when (val messageContent = message.messageContent) {
        is UIMessageContent.AssetMessage -> startAssetDeletion(
            expirableTimer = expirableTimer,
            onSelfDeletingMessageRead = { onStartMessageSelfDeletion(message) },
            downloadStatus = messageContent.downloadStatus
        )

        is UIMessageContent.AudioAssetMessage -> startAssetDeletion(
            expirableTimer = expirableTimer,
            onSelfDeletingMessageRead = { onStartMessageSelfDeletion(message) },
            downloadStatus = messageContent.downloadStatus
        )

        is UIMessageContent.ImageMessage -> startAssetDeletion(
            expirableTimer = expirableTimer,
            onSelfDeletingMessageRead = { onStartMessageSelfDeletion(message) },
            downloadStatus = messageContent.downloadStatus
        )

        else -> {
            LaunchedEffect(Unit) {
                onStartMessageSelfDeletion(message)
            }
            LaunchedEffect(expirableTimer.timeLeft) {
                with(expirableTimer) {
                    if (timeLeft != ZERO) {
                        delay(updateInterval())
                        decreaseTimeLeft(updateInterval())
                    }
                }
            }
        }
    }
}

@Composable
private fun startAssetDeletion(
    expirableTimer: SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable,
    onSelfDeletingMessageRead: () -> Unit,
    downloadStatus: Message.DownloadStatus
) {
    LaunchedEffect(downloadStatus) {
        if (downloadStatus == Message.DownloadStatus.SAVED_EXTERNALLY || downloadStatus == Message.DownloadStatus.SAVED_INTERNALLY) {
            onSelfDeletingMessageRead()
        }
    }
    LaunchedEffect(expirableTimer.timeLeft, downloadStatus) {
        if (downloadStatus == Message.DownloadStatus.SAVED_EXTERNALLY || downloadStatus == Message.DownloadStatus.SAVED_INTERNALLY) {
            with(expirableTimer) {
                if (timeLeft != ZERO) {
                    delay(updateInterval())
                    decreaseTimeLeft(updateInterval())
                }
            }
        }
    }
}
