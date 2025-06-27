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
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CALL_SCREEN_SHARE
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CONVERSATION_GUESTS
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CONVERSATION_GUESTS_PRO
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CONVERSATION_SERVICES
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CONVERSATION_SIZE
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_CONVERSATION_TYPE
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_END_REASON
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_ENDED_UNIQUE_SCREEN_SHARE
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_CALL_SCREEN_SHARE
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_CALL_TOO_SHORT
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_IGNORE_REASON
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_LABEL_ANSWERED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_LABEL_DISMISSED
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_LABEL_KEY
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALLING_QUALITY_REVIEW_SCORE_KEY
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALL_DURATION
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALL_PARTICIPANTS
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CALL_VIDEO
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.CONTRIBUTED_LOCATION
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.IS_TEAM_MEMBER
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.MESSAGE_ACTION_KEY
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.MIGRATION_DOT_ACTIVE
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.PERSONAL_TO_TEAM_FLOW_COMPLETED_EVENT
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.PERSONAL_TO_TEAM_FLOW_CONFIRM_EVENT
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.PERSONAL_TO_TEAM_FLOW_TEAM_NAME_EVENT
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.PERSONAL_TO_TEAM_FLOW_TEAM_PLAN_EVENT
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.QR_CODE_SEGMENTATION_USER_TYPE_PERSONAL
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.QR_CODE_SEGMENTATION_USER_TYPE_TEAM
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.REGISTRATION_ACCOUNT_CODE_VERIFICATION_EVENT
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.REGISTRATION_ACCOUNT_CODE_VERIFICATION_FAILED_EVENT
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.REGISTRATION_ACCOUNT_SETUP_PASSWORD_TRIES_SEGMENTATION
import com.wire.android.feature.analytics.model.AnalyticsEventConstants.REGISTRATION_ACCOUNT_TOU_EVENT
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

    data class AppOpen(val isTeamMember: Boolean?) : AnalyticsEvent {
        override val key: String = AnalyticsEventConstants.APP_OPEN
        override fun toSegmentation(): Map<String, Any> {
            return isTeamMember?.let {
                mapOf(IS_TEAM_MEMBER to it)
            } ?: super.toSegmentation()
        }
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
        val callDuration: Int
        val isTeamMember: Boolean
        val participantsCount: Int
        val isScreenSharedDuringCall: Boolean
        val isCameraEnabledDuringCall: Boolean

        override fun toSegmentation(): Map<String, Any> {
            return mapOf(
                CALLING_QUALITY_REVIEW_LABEL_KEY to label,
                CALL_DURATION to callDuration,
                IS_TEAM_MEMBER to isTeamMember,
                CALL_PARTICIPANTS to participantsCount,
                CALLING_QUALITY_REVIEW_CALL_SCREEN_SHARE to isScreenSharedDuringCall,
                CALL_VIDEO to isCameraEnabledDuringCall
            )
        }

        data class Answered(
            val score: Int,
            override val callDuration: Int,
            override val isTeamMember: Boolean,
            override val participantsCount: Int,
            override val isScreenSharedDuringCall: Boolean,
            override val isCameraEnabledDuringCall: Boolean
        ) : CallQualityFeedback {
            override val label: String
                get() = CALLING_QUALITY_REVIEW_LABEL_ANSWERED

            override fun toSegmentation(): Map<String, Any> {
                return mapOf(
                    CALLING_QUALITY_REVIEW_SCORE_KEY to score,
                    CALLING_QUALITY_REVIEW_LABEL_KEY to label,
                    CALL_DURATION to callDuration,
                    IS_TEAM_MEMBER to isTeamMember,
                    CALL_PARTICIPANTS to participantsCount,
                    CALLING_QUALITY_REVIEW_CALL_SCREEN_SHARE to isScreenSharedDuringCall,
                    CALL_VIDEO to isCameraEnabledDuringCall
                )
            }
        }

        data class TooShort(
            override val callDuration: Int,
            override val isTeamMember: Boolean,
            override val participantsCount: Int,
            override val isScreenSharedDuringCall: Boolean,
            override val isCameraEnabledDuringCall: Boolean
        ) : CallQualityFeedback {
            override val label: String
                get() = CALLING_QUALITY_REVIEW_CALL_TOO_SHORT
        }

        data class Muted(
            override val callDuration: Int,
            override val isTeamMember: Boolean,
            override val participantsCount: Int,
            override val isScreenSharedDuringCall: Boolean,
            override val isCameraEnabledDuringCall: Boolean
        ) : CallQualityFeedback {
            override val label: String
                get() = CALLING_QUALITY_REVIEW_IGNORE_REASON
        }

        data class Dismissed(
            override val callDuration: Int,
            override val isTeamMember: Boolean,
            override val participantsCount: Int,
            override val isScreenSharedDuringCall: Boolean,
            override val isCameraEnabledDuringCall: Boolean
        ) : CallQualityFeedback {
            override val label: String
                get() = CALLING_QUALITY_REVIEW_LABEL_DISMISSED
        }
    }

    data class RecentlyEndedCallEvent(val metadata: RecentlyEndedCallMetadata) : AnalyticsEvent {
        override val key: String = CALLING_ENDED

        override fun toSegmentation(): Map<String, Any> {
            return mapOf(
                IS_TEAM_MEMBER to metadata.isTeamMember,
                CALLING_ENDED_CALL_SCREEN_SHARE to metadata.callDetails.screenShareDurationInSeconds,
                CALLING_ENDED_UNIQUE_SCREEN_SHARE to metadata.callDetails.callScreenShareUniques,
                CALLING_ENDED_CALL_DIRECTION to metadata.toCallDirection(),
                CALL_DURATION to metadata.callDetails.callDurationInSeconds,
                CALLING_ENDED_CONVERSATION_TYPE to metadata.toConversationType(),
                CALLING_ENDED_CONVERSATION_SIZE to metadata.conversationDetails.conversationSize,
                CALLING_ENDED_CONVERSATION_GUESTS to metadata.conversationDetails.conversationGuests,
                CALLING_ENDED_CONVERSATION_GUESTS_PRO to metadata.conversationDetails.conversationGuestsPro,
                CALL_PARTICIPANTS to metadata.callDetails.callParticipantsCount,
                CALLING_ENDED_END_REASON to metadata.callEndReason,
                CALLING_ENDED_CONVERSATION_SERVICES to metadata.callDetails.conversationServices,
                CALLING_ENDED_AV_SWITCH_TOGGLE to metadata.callDetails.callAVSwitchToggle,
                CALL_VIDEO to metadata.callDetails.callVideoEnabled,
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
                Conversation.Type.OneOnOne -> "one_to_one"
                Conversation.Type.Group.Regular -> "group"
                Conversation.Type.Group.Channel -> "channel"
                else -> throw IllegalStateException("Call should not happen for ${conversationDetails.conversationType}")
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
        fun createSegmentationMap(
            isTeam: Boolean
        ): Map<String, Any> {
            val userType = if (isTeam) {
                QR_CODE_SEGMENTATION_USER_TYPE_TEAM
            } else {
                QR_CODE_SEGMENTATION_USER_TYPE_PERSONAL
            }

            return mapOf(
                AnalyticsEventConstants.QR_CODE_SEGMENTATION_USER_TYPE to userType,
                IS_TEAM_MEMBER to isTeam,
            )
        }

        data class Click(
            val isTeam: Boolean
        ) : QrCode() {
            override val key: String = AnalyticsEventConstants.QR_CODE_CLICK
            override fun toSegmentation(): Map<String, Any> = createSegmentationMap(isTeam)
        }

        sealed class Modal : QrCode() {
            data class Back(
                val isTeam: Boolean
            ) : Modal() {
                override val key: String = AnalyticsEventConstants.QR_CODE_MODAL_BACK
                override fun toSegmentation(): Map<String, Any> = createSegmentationMap(isTeam)
            }

            data class ShareProfileLink(
                val isTeam: Boolean
            ) : Modal() {
                override val key: String = AnalyticsEventConstants.QR_CODE_SHARE_PROFILE_LINK
                override fun toSegmentation(): Map<String, Any> = createSegmentationMap(isTeam)
            }

            data class ShareQrCode(
                val isTeam: Boolean
            ) : Modal() {
                override val key: String = AnalyticsEventConstants.QR_CODE_SHARE_QR_CODE
                override fun toSegmentation(): Map<String, Any> = createSegmentationMap(isTeam)
            }
        }
    }

    sealed interface PersonalTeamMigration : AnalyticsEvent {
        data class PersonalTeamCreationFlowTeamPlan(
            val isMigrationDotActive: Boolean
        ) : AnalyticsEvent {
            override val key: String = PERSONAL_TO_TEAM_FLOW_TEAM_PLAN_EVENT

            override fun toSegmentation(): Map<String, Any> {
                return mapOf(
                    MIGRATION_DOT_ACTIVE to isMigrationDotActive
                )
            }
        }

        data object PersonalTeamCreationFlowTeamName : AnalyticsEvent {
            override val key: String = PERSONAL_TO_TEAM_FLOW_TEAM_NAME_EVENT
        }

        data object PersonalTeamCreationFlowConfirm : AnalyticsEvent {
            override val key: String = PERSONAL_TO_TEAM_FLOW_CONFIRM_EVENT
        }

        data object PersonalTeamCreationFlowCompleted : AnalyticsEvent {
            override val key: String = PERSONAL_TO_TEAM_FLOW_COMPLETED_EVENT
        }
    }

    sealed interface RegistrationPersonalAccount : AnalyticsEvent {
        override fun toSegmentation(): Map<String, Any> = mapOf(IS_TEAM_MEMBER to false)

        data class AccountSetup(val withPasswordTries: Boolean) : RegistrationPersonalAccount {
            override val key: String = AnalyticsEventConstants.REGISTRATION_ACCOUNT_SETUP_EVENT
            override fun toSegmentation(): Map<String, Any> = mapOf(
                IS_TEAM_MEMBER to false,
                REGISTRATION_ACCOUNT_SETUP_PASSWORD_TRIES_SEGMENTATION to withPasswordTries
            )
        }

        data object TermsOfUseDialog : RegistrationPersonalAccount {
            override val key: String = REGISTRATION_ACCOUNT_TOU_EVENT
        }

        data object CodeVerification : RegistrationPersonalAccount {
            override val key: String = REGISTRATION_ACCOUNT_CODE_VERIFICATION_EVENT
        }

        data object CodeVerificationFailed : RegistrationPersonalAccount {
            override val key: String = REGISTRATION_ACCOUNT_CODE_VERIFICATION_FAILED_EVENT
        }

        data object Username : RegistrationPersonalAccount {
            override val key: String = AnalyticsEventConstants.REGISTRATION_ACCOUNT_USERNAME_EVENT
        }

        data object CreationCompleted : RegistrationPersonalAccount {
            override val key: String = AnalyticsEventConstants.REGISTRATION_ACCOUNT_COMPLETION_EVENT
        }
    }
}

object AnalyticsEventConstants {
    const val APP_NAME = "app_name"
    const val APP_NAME_ANDROID = "android"
    const val APP_VERSION = "app_version"
    const val OS_VERSION = "os_version"
    const val DEVICE_MODEL = "device_model"
    const val TEAM_IS_TEAM = "team_is_team"
    const val APP_OPEN = "app.open"

    const val IS_TEAM_MEMBER = "is_team_member"
    const val TEAM_IS_ENTERPRISE = "team_is_enterprise"
    const val TEAM_TEAM_ID = "team_team_id"
    const val TEAM_TEAM_SIZE = "team_team_size"
    const val USER_CONTACTS = "user_contacts"

    /**
     * Calling
     */
    const val CALLING_INITIATED = "calling.initiated_call"
    const val CALLING_JOINED = "calling.joined_call"
    const val CALLING_ENDED = "calling.ended_call"

    const val CALLING_QUALITY_REVIEW = "calling.call_quality_review"
    const val CALLING_QUALITY_REVIEW_LABEL_KEY = "label"
    const val CALLING_QUALITY_REVIEW_LABEL_ANSWERED = "answered"
    const val CALLING_QUALITY_REVIEW_LABEL_DISMISSED = "dismissed"
    const val CALLING_QUALITY_REVIEW_SCORE_KEY = "score"
    const val CALLING_QUALITY_REVIEW_IGNORE_REASON = "muted"
    const val CALLING_QUALITY_REVIEW_CALL_TOO_SHORT = "call_too_short"
    const val CALLING_QUALITY_REVIEW_CALL_SCREEN_SHARE = "call_screen_share"

    const val CALL_DURATION = "call_duration"
    const val CALL_PARTICIPANTS = "call_participants"
    const val CALL_VIDEO = "call_video"

    /**
     * Call ended
     */
    const val CALLING_ENDED_CALL_SCREEN_SHARE = "call_screen_share_duration"
    const val CALLING_ENDED_UNIQUE_SCREEN_SHARE = "call_screen_share_unique"
    const val CALLING_ENDED_CALL_DIRECTION = "call_direction"
    const val CALLING_ENDED_CONVERSATION_TYPE = "conversation_type"
    const val CALLING_ENDED_CONVERSATION_SIZE = "conversation_size"
    const val CALLING_ENDED_CONVERSATION_GUESTS = "conversation_guests"
    const val CALLING_ENDED_CONVERSATION_GUESTS_PRO = "conversation_guest_pro"
    const val CALLING_ENDED_END_REASON = "call_end_reason"
    const val CALLING_ENDED_CONVERSATION_SERVICES = "conversation_services"
    const val CALLING_ENDED_AV_SWITCH_TOGGLE = "call_av_switch_toggle"

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
    const val QR_CODE_MODAL_BACK = "user.back.share-profile"
    const val QR_CODE_SHARE_PROFILE_LINK = "user.share-profile"
    const val QR_CODE_SHARE_QR_CODE = "user.QR-code"

    const val QR_CODE_SEGMENTATION_USER_TYPE = "user_type"
    const val QR_CODE_SEGMENTATION_USER_TYPE_PERSONAL = "personal"
    const val QR_CODE_SEGMENTATION_USER_TYPE_TEAM = "team"

    /**
     * Personal to team migration
     */
    const val PERSONAL_TO_TEAM_FLOW_TEAM_PLAN_EVENT = "user.personal-to-team-flow-team-plan-1"
    const val PERSONAL_TO_TEAM_FLOW_TEAM_NAME_EVENT = "user.personal-to-team-flow-team-name-2"
    const val PERSONAL_TO_TEAM_FLOW_CONFIRM_EVENT = "user.personal-to-team-flow-confirm-3"
    const val PERSONAL_TO_TEAM_FLOW_COMPLETED_EVENT = "user.personal-to-team-flow-completed-4"
    const val MIGRATION_DOT_ACTIVE = "migration_dot_active"

    /**
     * New registration - Personal account creation
     */
    const val REGISTRATION_ACCOUNT_SETUP_EVENT = "registration.account_setup_screen_1"
    const val REGISTRATION_ACCOUNT_SETUP_PASSWORD_TRIES_SEGMENTATION = "multiple_password_tries"
    const val REGISTRATION_ACCOUNT_TOU_EVENT = "registration.account_ToU_screen_1.5"
    const val REGISTRATION_ACCOUNT_CODE_VERIFICATION_EVENT = "registration.account_verification_screen_2"
    const val REGISTRATION_ACCOUNT_CODE_VERIFICATION_FAILED_EVENT = "registration.account_verification_failed_screen_2.5"
    const val REGISTRATION_ACCOUNT_USERNAME_EVENT = "registration.account_username_screen_3"
    const val REGISTRATION_ACCOUNT_COMPLETION_EVENT = "registration.account_completion_screen_4"
}
