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

import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_AV_SWITCH_TOGGLE
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CALL_DIRECTION
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CALL_DURATION
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CALL_PARTICIPANTS
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CALL_SCREEN_SHARE
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CALL_VIDEO
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CONVERSATION_GUESTS
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CONVERSATION_GUESTS_PRO
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CONVERSATION_SERVICES
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CONVERSATION_SIZE
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CONVERSATION_TYPE
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_END_REASON
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_IS_TEAM_MEMBER
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_UNIQUE_SCREEN_SHARE
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
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.QR_CODE_SEGMENTATION_USER_TYPE_PERSONAL
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.QR_CODE_SEGMENTATION_USER_TYPE_TEAM
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.STEP_MODAL_CREATE_TEAM
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.USER_PROFILE_OPENED
import com.wire.kalium.logic.data.call.RecentlyEndedCallMetadata
import com.wire.kalium.logic.data.conversation.Conversation

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

    data class RecentlyEndedCallEvent(val metadata: RecentlyEndedCallMetadata) : AnalyticsEvent {
        override val key: String = CALLING_ENDED

        override fun toSegmentation(): Map<String, Any> {
            return mapOf(
                CALLING_ENDED_IS_TEAM_MEMBER to metadata.isTeamMember,
                CALLING_ENDED_CALL_SCREEN_SHARE to metadata.callDetails.screenShareDurationInSeconds,
                CALLING_ENDED_UNIQUE_SCREEN_SHARE to metadata.callDetails.callScreenShareUniques,
                CALLING_ENDED_CALL_DIRECTION to metadata.toCallDirection(),
                CALLING_ENDED_CALL_DURATION to metadata.callDetails.callDurationInSeconds,
                CALLING_ENDED_CONVERSATION_TYPE to metadata.toConversationType(),
                CALLING_ENDED_CONVERSATION_SIZE to metadata.conversationDetails.conversationSize,
                CALLING_ENDED_CONVERSATION_GUESTS to metadata.conversationDetails.conversationGuests,
                CALLING_ENDED_CONVERSATION_GUESTS_PRO to metadata.conversationDetails.conversationGuestsPro,
                CALLING_ENDED_CALL_PARTICIPANTS to metadata.callDetails.callParticipantsCount,
                CALLING_ENDED_END_REASON to metadata.callEndReason,
                CALLING_ENDED_CONVERSATION_SERVICES to metadata.callDetails.conversationServices,
                CALLING_ENDED_AV_SWITCH_TOGGLE to metadata.callDetails.callAVSwitchToggle,
                CALLING_ENDED_CALL_VIDEO to metadata.callDetails.callVideoEnabled,
            )
        }

        private fun RecentlyEndedCallMetadata.toCallDirection(): String {
            return if (callDetails.isOutgoingCall) {
                "outgoing"
            } else {
                "incoming"
            }
        }

        private fun RecentlyEndedCallMetadata.toConversationType(): String {
            return when (conversationDetails.conversationType) {
                Conversation.Type.ONE_ON_ONE -> "one_to_one"
                Conversation.Type.GROUP -> "group"
                else -> "unknown"
            }
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

    sealed class QrCode : AnalyticsEvent {
        data class Click(val isTeam: Boolean) : QrCode() {
            override val key: String = AnalyticsEventConstants.QR_CODE_CLICK

            override fun toSegmentation(): Map<String, Any> {
                val userType = if (isTeam) {
                    QR_CODE_SEGMENTATION_USER_TYPE_TEAM
                } else {
                    QR_CODE_SEGMENTATION_USER_TYPE_PERSONAL
                }

                return mapOf(
                    AnalyticsEventConstants.QR_CODE_SEGMENTATION_USER_TYPE to userType
                )
            }
        }

        sealed class Modal : QrCode() {
            data object Displayed : Modal() {
                override val key: String = AnalyticsEventConstants.QR_CODE_MODAL
            }

            data object Back : Modal() {
                override val key: String = AnalyticsEventConstants.QR_CODE_MODAL_BACK
            }

            data object ShareProfileLink : Modal() {
                override val key: String = AnalyticsEventConstants.QR_CODE_SHARE_PROFILE_LINK
            }

            data object ShareQrCode : Modal() {
                override val key: String = AnalyticsEventConstants.QR_CODE_SHARE_QR_CODE
            }
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
                val segmentations = mutableMapOf<String, Boolean>()
                createTeamButtonClicked?.let {
                    segmentations.put(CLICKED_CREATE_TEAM, it)
                }
                dismissCreateTeamButtonClicked?.let {
                    segmentations.put(CLICKED_DISMISS_CTA, it)
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
                val segmentations = mutableMapOf<String, Any>()
                modalLeaveClicked?.let {
                    segmentations.put(MODAL_LEAVE_CLICKED, it)
                }
                modalContinueClicked?.let {
                    segmentations.put(MODAL_CONTINUE_CLICKED, it)
                }
                teamName?.let {
                    segmentations.put(MODAL_TEAM_NAME, it)
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
                val segmentations = mutableMapOf<String, Any>()
                teamName?.let {
                    segmentations.put(MODAL_TEAM_NAME, it)
                }
                modalOpenTeamManagementButtonClicked?.let {
                    segmentations.put(MODAL_OPEN_TEAM_MANAGEMENT_CLICKED, it)
                }
                backToWireButtonClicked?.let {
                    segmentations.put(MODAL_BACK_TO_WIRE_CLICKED, it)
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
    const val CALLING_ENDED = "calling.ended_call"

    const val CALLING_QUALITY_REVIEW = "calling.call_quality_review"
    const val CALLING_QUALITY_REVIEW_LABEL_KEY = "label"
    const val CALLING_QUALITY_REVIEW_LABEL_ANSWERED = "answered"
    const val CALLING_QUALITY_REVIEW_LABEL_NOT_DISPLAYED = "not-displayed"
    const val CALLING_QUALITY_REVIEW_LABEL_DISMISSED = "dismissed"
    const val CALLING_QUALITY_REVIEW_SCORE_KEY = "score"
    const val CALLING_QUALITY_REVIEW_IGNORE_REASON_KEY = "ignore-reason"
    const val CALLING_QUALITY_REVIEW_IGNORE_REASON = "muted"

    /**
     * Call ended
     */
    const val CALLING_ENDED_IS_TEAM_MEMBER = "is_team_member"
    const val CALLING_ENDED_CALL_SCREEN_SHARE = "call_screen_share_duration"
    const val CALLING_ENDED_UNIQUE_SCREEN_SHARE = "call_screen_share_unique"
    const val CALLING_ENDED_CALL_DIRECTION = "call_direction"
    const val CALLING_ENDED_CALL_DURATION = "call_duration"
    const val CALLING_ENDED_CONVERSATION_TYPE = "conversation_type"
    const val CALLING_ENDED_CONVERSATION_SIZE = "conversation_size"
    const val CALLING_ENDED_CONVERSATION_GUESTS = "conversation_guests"
    const val CALLING_ENDED_CONVERSATION_GUESTS_PRO = "conversation_guest_pro"
    const val CALLING_ENDED_CALL_PARTICIPANTS = "call_participants"
    const val CALLING_ENDED_END_REASON = "call_end_reason"
    const val CALLING_ENDED_CONVERSATION_SERVICES = "conversation_services"
    const val CALLING_ENDED_AV_SWITCH_TOGGLE = "call_av_switch_toggle"
    const val CALLING_ENDED_CALL_VIDEO = "call_video"

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
     * Qr code
     */
    const val QR_CODE_CLICK = "ui.QR-click"
    const val QR_CODE_MODAL = "ui.share.profile"
    const val QR_CODE_MODAL_BACK = "user.back.share-profile"
    const val QR_CODE_SHARE_PROFILE_LINK = "user.share-profile"
    const val QR_CODE_SHARE_QR_CODE = "user.QR-code"

    const val QR_CODE_SEGMENTATION_USER_TYPE = "user_type"
    const val QR_CODE_SEGMENTATION_USER_TYPE_PERSONAL = "personal"
    const val QR_CODE_SEGMENTATION_USER_TYPE_TEAM = "team"

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
