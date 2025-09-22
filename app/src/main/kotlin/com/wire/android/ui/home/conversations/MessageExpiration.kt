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

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.wire.android.R
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable.Companion.HIGH_END_TIME_ELAPSED_RATIO_BOUNDARY_FOR_PROPORTIONAL_ALPHA_CHANGE
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable.Companion.LOW_END_TIME_ELAPSED_RATIO_BOUNDARY_FOR_PROPORTIONAL_ALPHA_CHANGE
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.logic.data.message.Message
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun rememberSelfDeletionTimer(expirationStatus: ExpirationStatus): SelfDeletionTimerHelper.SelfDeletionTimerState {
    val stringResourceProvider: StringResourceProvider = stringResourceProvider()
    val currentTimeProvider: CurrentTimeProvider = { Clock.System.now() }

    return remember((expirationStatus as? ExpirationStatus.Expirable)?.selfDeletionStatus ?: true) {
        SelfDeletionTimerHelper(stringResourceProvider, currentTimeProvider)
            .fromExpirationStatus(expirationStatus)
    }
}

@Composable
private fun stringResourceProvider(): StringResourceProvider {
    with(LocalContext.current.resources) {
        return object : StringResourceProvider {
            override fun quantityString(type: StringResourceType, quantity: Int, withLeftText: Boolean): String = if (withLeftText) {
                getQuantityString(
                    when (type) {
                        StringResourceType.WEEKS -> R.plurals.weeks_left
                        StringResourceType.DAYS -> R.plurals.days_left
                        StringResourceType.HOURS -> R.plurals.hours_left
                        StringResourceType.MINUTES -> R.plurals.minutes_left
                        StringResourceType.SECONDS -> R.plurals.seconds_left
                    },
                    quantity,
                    quantity
                )
            } else {
                getQuantityString(
                    when (type) {
                        StringResourceType.WEEKS -> R.plurals.weeks
                        StringResourceType.DAYS -> R.plurals.days
                        StringResourceType.HOURS -> R.plurals.hours
                        StringResourceType.MINUTES -> R.plurals.minutes
                        StringResourceType.SECONDS -> R.plurals.seconds
                    },
                    quantity,
                    quantity
                )
            }
        }
    }
}

class SelfDeletionTimerHelper(private val stringResourceProvider: StringResourceProvider, private val currentTime: CurrentTimeProvider) {

    fun fromExpirationStatus(expirationStatus: ExpirationStatus): SelfDeletionTimerState {
        return if (expirationStatus is ExpirationStatus.Expirable) {
            with(expirationStatus) {
                val expireAt = calculateExpireAt(selfDeletionStatus, expireAfter)
                SelfDeletionTimerState.Expirable(
                    stringResourceProvider,
                    expireAfter,
                    expireAt,
                    currentTime
                )
            }
        } else {
            SelfDeletionTimerState.NotExpirable
        }
    }

    private fun calculateExpireAt(
        selfDeletionStatus: Message.ExpirationData.SelfDeletionStatus?,
        expireAfter: Duration,
    ) =
        if (selfDeletionStatus is Message.ExpirationData.SelfDeletionStatus.Started) {
            selfDeletionStatus.selfDeletionEndDate
        } else {
            currentTime() + expireAfter
        }

    sealed class SelfDeletionTimerState {

        class Expirable(
            private val stringResourceProvider: StringResourceProvider,
            private val expireAfter: Duration,
            private val expireAt: Instant,
            private val currentTime: CurrentTimeProvider,
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

            var timeLeft by mutableStateOf(calculateTimeLeft())
                private set

            val fractionLeft: Float by derivedStateOf {
                ((timeLeft / expireAfter).toFloat()).coerceIn(0f, 1f)
            }

            @Suppress("MagicNumber", "ComplexMethod")
            val timeLeftFormatted: String by derivedStateOf {
                when {
                    timeLeft > 28.days ->
                        stringResourceProvider.quantityString(StringResourceType.WEEKS, 4)
                    // 4 weeks
                    timeLeft >= 27.days && timeLeft <= 28.days ->
                        stringResourceProvider.quantityString(StringResourceType.WEEKS, 4)
                    // days below 4 weeks
                    timeLeft <= 27.days && timeLeft > 7.days ->
                        stringResourceProvider.quantityString(StringResourceType.DAYS, timeLeft.inWholeDays.toInt())
                    // one week
                    timeLeft >= 6.days && timeLeft <= 7.days ->
                        stringResourceProvider.quantityString(StringResourceType.WEEKS, 1)
                    // days below 1 week
                    timeLeft < 7.days && timeLeft >= 1.days ->
                        stringResourceProvider.quantityString(StringResourceType.DAYS, timeLeft.inWholeDays.toInt())
                    // hours below one day
                    timeLeft >= 1.hours && timeLeft < 24.hours ->
                        stringResourceProvider.quantityString(StringResourceType.HOURS, timeLeft.inWholeHours.toInt())
                    // minutes below hour
                    timeLeft >= 1.minutes && timeLeft < 60.minutes ->
                        stringResourceProvider.quantityString(StringResourceType.MINUTES, timeLeft.inWholeMinutes.toInt())
                    // seconds below minute
                    timeLeft < 60.seconds ->
                        stringResourceProvider.quantityString(StringResourceType.SECONDS, timeLeft.inWholeSeconds.toInt())

                    else -> throw IllegalStateException("Not possible state for a time left label")
                }
            }

            @Suppress("MagicNumber", "ComplexMethod")
            val timeFormatted: String by derivedStateOf {
                when {
                    timeLeft > 28.days ->
                        stringResourceProvider.quantityString(StringResourceType.WEEKS, 4, false)
                    // 4 weeks
                    timeLeft >= 27.days && timeLeft <= 28.days ->
                        stringResourceProvider.quantityString(StringResourceType.WEEKS, 4, false)
                    // days below 4 weeks
                    timeLeft <= 27.days && timeLeft > 7.days ->
                        stringResourceProvider.quantityString(StringResourceType.DAYS, timeLeft.inWholeDays.toInt(), false)
                    // one week
                    timeLeft >= 6.days && timeLeft <= 7.days ->
                        stringResourceProvider.quantityString(StringResourceType.WEEKS, 1, false)
                    // days below 1 week
                    timeLeft < 7.days && timeLeft >= 1.days ->
                        stringResourceProvider.quantityString(StringResourceType.DAYS, timeLeft.inWholeDays.toInt(), false)
                    // hours below one day
                    timeLeft >= 1.hours && timeLeft < 24.hours ->
                        stringResourceProvider.quantityString(StringResourceType.HOURS, timeLeft.inWholeHours.toInt(), false)
                    // minutes below hour
                    timeLeft >= 1.minutes && timeLeft < 60.minutes ->
                        stringResourceProvider.quantityString(StringResourceType.MINUTES, timeLeft.inWholeMinutes.toInt(), false)
                    // seconds below minute
                    timeLeft < 60.seconds ->
                        stringResourceProvider.quantityString(StringResourceType.SECONDS, timeLeft.inWholeSeconds.toInt(), false)

                    else -> throw IllegalStateException("Not possible state for a time label")
                }
            }

            /**
             * Calculates when the next update should be done based on the [timeLeft]
             * For example, it's unnecessary to update on each second when the timer
             * is still marking 4 weeks left.
             * However, when there's less than a minute left, the timer should be
             * updated every second.
             * @return how long until the next timer update.
             */
            @VisibleForTesting
            internal fun updateInterval(): Duration {
                fun remainingTimeToDurationUnit(durationUnit: DurationUnit): Duration {
                    /*
                     * Function toLong returns the whole part for the given duration unit and then this whole value is converted back to
                     * Duration and subtracted from the original duration, which gives the remaining time to the next full duration unit.
                     *
                     * For example, if the time left is "1 day and 1 hour" and durationUnit is DAYS, then toLong will return 1L
                     * which means "1 full day" (just like .inWholeDays) and then it will be converted back to Duration type.
                     * Then this "1 day" will be subtracted from the original duration, returning "1 hour" left ("1d 1h" - "1d" = "1h").
                     * So in this case it's the same as `timeLeft - timeLeft.inWholeHours.hours`
                     * because `timeLeft.inWholeDays` is basically `timeLeft.toLong(DurationUnit.DAYS)`
                     * and `1L.days` is the same as `1L.toDuration(DurationUnit.DAYS)`.
                     */
                    val timeLeftForDurationUnit = timeLeft - timeLeft.toLong(durationUnit).toDuration(durationUnit)
                    return if (timeLeftForDurationUnit == ZERO) 1.toDuration(durationUnit)
                    else timeLeftForDurationUnit
                }

                val timeLeftUpdateInterval = when {
                    timeLeft > 24.hours -> remainingTimeToDurationUnit(DurationUnit.DAYS)
                    timeLeft <= 24.hours && timeLeft > 1.hours -> remainingTimeToDurationUnit(DurationUnit.HOURS)
                    timeLeft <= 1.hours && timeLeft > 1.minutes -> remainingTimeToDurationUnit(DurationUnit.MINUTES)
                    timeLeft <= 1.minutes -> remainingTimeToDurationUnit(DurationUnit.SECONDS)
                    else -> throw IllegalStateException("Not possible state for the interval")
                }

                return timeLeftUpdateInterval
            }

            // non-negative value, returns ZERO if message is already expired
            private fun calculateTimeLeft(): Duration = (expireAt - currentTime()).let { if (it.isNegative()) ZERO else it }

            @VisibleForTesting
            internal fun recalculateTimeLeft() {
                timeLeft = calculateTimeLeft()
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

            @Composable
            fun StartDeletionTimer(message: UIMessage, onSelfDeletingMessageRead: (UIMessage) -> Unit) {
                LaunchedEffect(Unit) {
                    onSelfDeletingMessageRead(message)
                }
                LaunchedEffect(timeLeft) {
                    if (timeLeft != ZERO) {
                        delay(updateInterval())
                        recalculateTimeLeft()
                    }
                }
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        recalculateTimeLeft()
                    }
                }
            }
        }

        data object NotExpirable : SelfDeletionTimerState()
    }
}

typealias CurrentTimeProvider = () -> Instant

enum class StringResourceType { WEEKS, DAYS, HOURS, MINUTES, SECONDS; }
interface StringResourceProvider {
    fun quantityString(type: StringResourceType, quantity: Int, withLeftText: Boolean = true): String
}
