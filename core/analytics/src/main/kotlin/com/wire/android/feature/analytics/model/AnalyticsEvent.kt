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
package com.wire.android.feature.analytics.model

import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_LABEL_ANSWERED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_LABEL_DISMISSED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_LABEL_KEY
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_LABEL_NOT_DISPLAYED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_SCORE_KEY
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CONTRIBUTED_LOCATION
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.MESSAGE_ACTION_KEY

interface AnalyticsEvent {
    /**
     * Key to be used to differentiate every event
     */
    val key: String

    /**
     * Each AnalyticsEvent must implement its own attributes.
     * This method is to be implemented as a Map<String, Any> based
     * on each attribute.
     *
     * Example:
     * data class ExampleEvent(
     *      override val key: String,
     *      val attr1: String,
     *      val attr2: String
     * ) : AnalyticsEvent() {
     *      override fun toSegmentation() {
     *          return mapOf(
     *              "attr1" to attr1,
     *              "attr2" to attr2
     *          )
     *      }
     * }
     */
    fun toSegmentation(): Map<String, Any> = mapOf()

    data object AppOpen : AnalyticsEvent {
        override val key: String = AnalyticsEventConstants.APP_OPEN
    }

    /**
     * Calling
     */
    data object CallInitiated : AnalyticsEvent {
        override val key: String = AnalyticsEventConstants.CALLING_INITIATED
    }

    data object CallJoined : AnalyticsEvent {
        override val key: String = AnalyticsEventConstants.CALLING_JOINED
    }

    /**
     * Call quality feedback
     */
    sealed interface CallQualityFeedback : AnalyticsEvent {
        override val key: String
            get() = AnalyticsEventConstants.CALLING_QUALITY_REVIEW
    }

    /**
     * Call quality feedback label
     */
    sealed interface CallQualityFeedbackLabel : CallQualityFeedback {
        val label: String

        override fun toSegmentation(): Map<String, Any> {
            return mapOf(
                CALLING_QUALITY_REVIEW_LABEL_KEY to label
            )
        }

        data object Answered : CallQualityFeedbackLabel {
            override val label: String
                get() = CALLING_QUALITY_REVIEW_LABEL_ANSWERED
        }

        data object NotDisplayed : CallQualityFeedbackLabel {
            override val label: String
                get() = CALLING_QUALITY_REVIEW_LABEL_NOT_DISPLAYED
        }

        data object Dismissed : CallQualityFeedbackLabel {
            override val label: String
                get() = CALLING_QUALITY_REVIEW_LABEL_DISMISSED
        }
    }

    /**
     * Call quality feedback score
     */
    sealed interface CallQualityFeedbackScore : CallQualityFeedback {
        val score: Int

        override fun toSegmentation(): Map<String, Any> {
            return mapOf(
                CALLING_QUALITY_REVIEW_SCORE_KEY to score
            )
        }

        data class Score(override val score: Int) : CallQualityFeedbackScore
    }

    /**
     * Backup
     */
    data object BackupExportFailed : AnalyticsEvent {
        override val key: String = AnalyticsEventConstants.BACKUP_EXPORT_FAILED
    }

    data object BackupRestoreSucceeded : AnalyticsEvent {
        override val key: String = AnalyticsEventConstants.BACKUP_RESTORE_SUCCEEDED
    }

    data object BackupRestoreFailed : AnalyticsEvent {
        override val key: String = AnalyticsEventConstants.BACKUP_RESTORE_FAILED
    }

    /**
     * Contributed, message action related
     */
    sealed interface Contributed : AnalyticsEvent {
        override val key: String
            get() = AnalyticsEventConstants.CONTRIBUTED

        val messageAction: String

        override fun toSegmentation(): Map<String, Any> {
            return mapOf(
                MESSAGE_ACTION_KEY to messageAction
            )
        }

        data object Location : Contributed {
            override val messageAction: String = CONTRIBUTED_LOCATION
        }

        data object Text : Contributed {
            override val messageAction: String = AnalyticsEventConstants.CONTRIBUTED_TEXT
        }

        data object Photo : Contributed {
            override val messageAction: String = AnalyticsEventConstants.CONTRIBUTED_PHOTO
        }

        data object AudioCall : Contributed {
            override val messageAction: String = AnalyticsEventConstants.CONTRIBUTED_AUDIO_CALL
        }

        data object VideoCall : Contributed {
            override val messageAction: String = AnalyticsEventConstants.CONTRIBUTED_VIDEO_CALL
        }

        data object Gif : Contributed {
            override val messageAction: String = AnalyticsEventConstants.CONTRIBUTED_GIF
        }

        data object Ping : Contributed {
            override val messageAction: String = AnalyticsEventConstants.CONTRIBUTED_PING
        }

        data object File : Contributed {
            override val messageAction: String = AnalyticsEventConstants.CONTRIBUTED_FILE
        }

        data object Video : Contributed {
            override val messageAction: String = AnalyticsEventConstants.CONTRIBUTED_VIDEO
        }

        data object Audio : Contributed {
            override val messageAction: String = AnalyticsEventConstants.CONTRIBUTED_AUDIO
        }
    }
}

object AnalyticsEventConstants {
    const val APP_NAME = "app_name"
    const val APP_NAME_ANDROID = "android"
    const val APP_VERSION = "app_version"
    const val TEAM_IS_TEAM = "team_is_team"
    const val APP_OPEN = "app.open"

    /**
     * Calling
     */
    const val CALLING_INITIATED = "calling.initiated_call"
    const val CALLING_JOINED = "calling.joined_call"

    const val CALLING_QUALITY_REVIEW = "calling.call_quality_review"
    const val CALLING_QUALITY_REVIEW_LABEL_KEY = "label"
    const val CALLING_QUALITY_REVIEW_LABEL_ANSWERED = "answered"
    const val CALLING_QUALITY_REVIEW_LABEL_NOT_DISPLAYED = "not-displayed"
    const val CALLING_QUALITY_REVIEW_LABEL_DISMISSED = "dismissed"
    const val CALLING_QUALITY_REVIEW_SCORE_KEY = "score"

    /**
     * Backup
     */
    const val BACKUP_EXPORT_FAILED = "backup.export_failed"
    const val BACKUP_RESTORE_SUCCEEDED = "backup.restore_succeeded"
    const val BACKUP_RESTORE_FAILED = "backup.restore_failed"

    /**
     * Contributed, message related
     */
    const val CONTRIBUTED = "contributed"
    const val MESSAGE_ACTION_KEY = "message_action"

    const val CONTRIBUTED_TEXT = "text"
    const val CONTRIBUTED_PHOTO = "photo"
    const val CONTRIBUTED_AUDIO_CALL = "audio_call"
    const val CONTRIBUTED_VIDEO_CALL = "video_call"
    const val CONTRIBUTED_GIF = "giphy"
    const val CONTRIBUTED_PING = "ping"
    const val CONTRIBUTED_FILE = "file"
    const val CONTRIBUTED_VIDEO = "video"
    const val CONTRIBUTED_AUDIO = "audio"
    const val CONTRIBUTED_LOCATION = "location"
}
