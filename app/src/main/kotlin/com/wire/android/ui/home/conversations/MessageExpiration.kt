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
fun rememberSelfDeletionTimer(expirationStatus: ExpirationStatus): SelfDeletionTimer.SelfDeletionTimerState {
    val context = LocalContext.current

    return remember { SelfDeletionTimer(context).fromExpirationStatus(expirationStatus) }
}

class SelfDeletionTimer(private val context: Context) {

    fun fromExpirationStatus(expirationStatus: ExpirationStatus): SelfDeletionTimerState {
        return if (expirationStatus is ExpirationStatus.Expirable) {
            with(expirationStatus) {
                val timeLeft = calculateTimeLeft(selfDeletionStatus, expireAfter)
                SelfDeletionTimerState.Expirable(context.resources, timeLeft, expireAfter)
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

            // time left for deletion it can be a negative value if the time difference between the self deletion start date and
            // now is greater then expire after millis, we normalize it to 0 seconds
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
            private val expireAfter: Duration
        ) : SelfDeletionTimerState() {
            companion object {
                private const val TIME_LEFT_RATIO_BOUNDARY_FOR_ALMOST_TIME_LEFT_ELAPSED_ALPHA = 0.75
                private const val PAST_RATIO_BOUNDARY_FOR_ALMOST_TIME_LEFT_ALPHA_VALUE = 0F
                private const val BEFORE_RATIO_BOUNDARY_FOR_ALMOST_TIME_LEFT_ALPHA_VALUE = 1F
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

@Composable
fun startDeletionTimer(
    message: UIMessage,
    expirableTimer: SelfDeletionTimer.SelfDeletionTimerState.Expirable,
    onStartMessageSelfDeletion: (UIMessage) -> Unit
) {
    message.messageContent?.let {
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

            is UIMessageContent.TextMessage -> {
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

            else -> {}
        }
    }
}

@Composable
private fun startAssetDeletion(
    expirableTimer: SelfDeletionTimer.SelfDeletionTimerState.Expirable,
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
