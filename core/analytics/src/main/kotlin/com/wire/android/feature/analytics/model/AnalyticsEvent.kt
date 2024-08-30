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

    data class AppOpen(
        override val key: String = AnalyticsEventConstants.APP_OPEN
    ) : AnalyticsEvent

    /**
     * Calling
     */
    data class CallInitiated(
        override val key: String = AnalyticsEventConstants.CALLING_INITIATED
    ) : AnalyticsEvent

    data class CallJoined(
        override val key: String = AnalyticsEventConstants.CALLING_JOINED
    ) : AnalyticsEvent

    /**
     * Backup
     */
    data class BackupExportFailed(
        override val key: String = AnalyticsEventConstants.BACKUP_EXPORT_FAILED
    ) : AnalyticsEvent

    data class BackupRestoreSucceeded(
        override val key: String = AnalyticsEventConstants.BACKUP_RESTORE_SUCCEEDED
    ) : AnalyticsEvent

    data class BackupRestoreFailed(
        override val key: String = AnalyticsEventConstants.BACKUP_RESTORE_FAILED
    ) : AnalyticsEvent
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

    /**
     * Backup
     */
    const val BACKUP_EXPORT_FAILED = "backup.export_failed"
    const val BACKUP_RESTORE_SUCCEEDED = "backup.restore_succeeded"
    const val BACKUP_RESTORE_FAILED = "backup.restore_failed"
}
