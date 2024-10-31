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

import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_IGNORE_REASON
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_IGNORE_REASON_KEY
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_LABEL_ANSWERED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_LABEL_DISMISSED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_LABEL_KEY
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_LABEL_NOT_DISPLAYED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_SCORE_KEY
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CLICKED_CREATE_TEAM
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CLICKED_DISMISS_CTA
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CLICKED_PERSONAL_MIGRATION_CTA_EVENT
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CONTRIBUTED_LOCATION
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.MESSAGE_ACTION_KEY
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.MIGRATION_DOT_ACTIVE
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.MODAL_BACK_TO_WIRE_CLICKED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.MODAL_CONTINUE_CLICKED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.MODAL_LEAVE_CLICKED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.MODAL_OPEN_TEAM_MANAGEMENT_CLICKED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.MODAL_TEAM_NAME
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.PERSONAL_TEAM_CREATION_FLOW_CANCELLED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.PERSONAL_TEAM_CREATION_FLOW_COMPLETED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.PERSONAL_TEAM_CREATION_FLOW_STARTED_EVENT
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.STEP_MODAL_CREATE_TEAM
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.USER_PROFILE_OPENED

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
        val label: String

        override fun toSegmentation(): Map<String, Any> {
            return mapOf(
                CALLING_QUALITY_REVIEW_LABEL_KEY to label
            )
        }

        data class Answered(val score: Int) : CallQualityFeedback {
            override val label: String
                get() = CALLING_QUALITY_REVIEW_LABEL_ANSWERED

            override fun toSegmentation(): Map<String, Any> {
                return mapOf(
                    CALLING_QUALITY_REVIEW_LABEL_KEY to label,
                    CALLING_QUALITY_REVIEW_SCORE_KEY to score
                )
            }
        }

        data object NotDisplayed : CallQualityFeedback {
            override val label: String
                get() = CALLING_QUALITY_REVIEW_LABEL_NOT_DISPLAYED

            override fun toSegmentation(): Map<String, Any> {
                return mapOf(
                    CALLING_QUALITY_REVIEW_LABEL_KEY to label,
                    CALLING_QUALITY_REVIEW_IGNORE_REASON_KEY to CALLING_QUALITY_REVIEW_IGNORE_REASON
                )
            }
        }

        data object Dismissed : CallQualityFeedback {
            override val label: String
                get() = CALLING_QUALITY_REVIEW_LABEL_DISMISSED
        }
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

    data class UserProfileOpened(val isMigrationDotActive: Boolean) : AnalyticsEvent {
        override val key: String = USER_PROFILE_OPENED

        override fun toSegmentation(): Map<String, Any> {
            return mapOf(
                MIGRATION_DOT_ACTIVE to isMigrationDotActive
            )
        }
    }

    sealed interface PersonalTeamMigration : AnalyticsEvent {

        data class ClickedPersonalTeamMigrationCta(
            val createTeamButtonClicked: Boolean? = null,
            val dismissCreateTeamButtonClicked: Boolean? = null
        ) : AnalyticsEvent {
            override val key: String = CLICKED_PERSONAL_MIGRATION_CTA_EVENT

            override fun toSegmentation(): Map<String, Any> {
                val segmentations = mapOf<String, Boolean>()
                createTeamButtonClicked?.let {
                    segmentations.plus(CLICKED_CREATE_TEAM to it)
                }
                dismissCreateTeamButtonClicked?.let {
                    segmentations.plus(CLICKED_DISMISS_CTA to it)
                }
                return segmentations
            }
        }

        data class PersonalTeamCreationFlowStarted(
            val step: Int
        ) : AnalyticsEvent {
            override val key: String = PERSONAL_TEAM_CREATION_FLOW_STARTED_EVENT

            override fun toSegmentation(): Map<String, Any> {
                return mapOf(
                    STEP_MODAL_CREATE_TEAM to step
                )
            }
        }

        data class PersonalTeamCreationFlowCanceled(
            val teamName: String?,
            val modalLeaveClicked: Boolean? = null,
            val modalContinueClicked: Boolean? = null
        ) : AnalyticsEvent {
            override val key: String = PERSONAL_TEAM_CREATION_FLOW_CANCELLED

            override fun toSegmentation(): Map<String, Any> {
                val segmentations = mapOf<String, Any>()
                modalLeaveClicked?.let {
                    segmentations.plus(MODAL_LEAVE_CLICKED to it)
                }
                modalContinueClicked?.let {
                    segmentations.plus(MODAL_CONTINUE_CLICKED to it)
                }
                teamName?.let {
                    segmentations.plus(MODAL_TEAM_NAME to it)
                }
                return segmentations
            }
        }

        data class PersonalTeamCreationFlowCompleted(
            val teamName: String? = null,
            val modalOpenTeamManagementButtonClicked: Boolean? = null,
            val backToWireButtonClicked: Boolean? = null
        ) : AnalyticsEvent {
            override val key: String = PERSONAL_TEAM_CREATION_FLOW_COMPLETED

            override fun toSegmentation(): Map<String, Any> {
                val segmentations = mapOf<String, Any>()
                teamName?.let {
                    segmentations.plus(MODAL_TEAM_NAME to it)
                }
                modalOpenTeamManagementButtonClicked?.let {
                    segmentations.plus(MODAL_OPEN_TEAM_MANAGEMENT_CLICKED to it)
                }
                backToWireButtonClicked?.let {
                    segmentations.plus(MODAL_BACK_TO_WIRE_CLICKED to it)
                }
                return segmentations
            }
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
    const val CALLING_QUALITY_REVIEW_IGNORE_REASON_KEY = "ignore-reason"
    const val CALLING_QUALITY_REVIEW_IGNORE_REASON = "muted"

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

    /**
     * user profile
     */
    const val USER_PROFILE_OPENED = "ui.clicked-profile"

    /**
     * Personal to team migration
     */
    const val CLICKED_PERSONAL_MIGRATION_CTA_EVENT = "ui.clicked-personal-migration-cta"
    const val PERSONAL_TEAM_CREATION_FLOW_STARTED_EVENT = "user.personal-team-creation-flow-started"
    const val PERSONAL_TEAM_CREATION_FLOW_CANCELLED = "user.personal-team-creation-flow-cancelled"
    const val PERSONAL_TEAM_CREATION_FLOW_COMPLETED = "user.personal-team-creation-flow-completed"
    const val MIGRATION_DOT_ACTIVE = "migration_dot_active"
    const val CLICKED_CREATE_TEAM = "clicked_create_team"
    const val CLICKED_DISMISS_CTA = "clicked_dismiss_cta"
    const val STEP_MODAL_CREATE_TEAM = "step_modalcreateteam"
    const val MODAL_TEAM_NAME = "modal_team-name"
    const val MODAL_CONTINUE_CLICKED = "modal_continue-clicked"
    const val MODAL_LEAVE_CLICKED = "modal_leave-clicked"
    const val MODAL_BACK_TO_WIRE_CLICKED = "modal_back-to-wire-clicked"
    const val MODAL_OPEN_TEAM_MANAGEMENT_CLICKED = "modal_open-tm-clicked"
}
