/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.feature.calling

import android.content.Context
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ApplicationContext
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.userprofile.self.status.buildTextStatus
import com.wire.kalium.common.functional.fold
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.ConcurrentHashMap

@SingleIn(AppScope::class)
class AutoCallStatusObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val userDataStoreProvider: UserDataStoreProvider,
) {

    private val userMutexes = ConcurrentHashMap<UserId, Mutex>()

    suspend fun observe() {
        coreLogic.getGlobalScope().observeValidAccounts().collectLatest { validAccounts ->
            kotlinx.coroutines.coroutineScope {
                validAccounts.forEach { (selfUser, team) ->
                    launch {
                        observeAccountCallStatus(selfUser.id, team != null)
                    }
                }
            }
        }
    }

    private suspend fun observeAccountCallStatus(userId: UserId, isTeamMember: Boolean) {
        val sessionScope = coreLogic.getSessionScope(userId)
        combine(
            sessionScope.calls.establishedCall(),
            sessionScope.users.observeSelfUserWithTeam()
        ) { establishedCalls, selfWithTeam ->
            AccountCallStatusState(
                isTeamMember = isTeamMember && selfWithTeam.second != null,
                establishedCalls = establishedCalls,
                selfUser = selfWithTeam.first,
            )
        }.collectLatest { state ->
            reconcile(userId, state)
        }
    }

    private suspend fun reconcile(userId: UserId, state: AccountCallStatusState) {
        userMutex(userId).withLock {
            val dataStore = userDataStoreProvider.getOrCreate(userId)
            val snapshot = dataStore.getAutoCallStatusSnapshot()
            val hasEstablishedCall = state.establishedCalls.isNotEmpty()

            if (!state.isTeamMember) {
                if (snapshot != null) {
                    if (restoreSnapshot(userId, state.selfUser, snapshot.availabilityStatus, snapshot.textStatus)) {
                        dataStore.clearAutoCallStatusSnapshot()
                    }
                }
                return
            }

            if (hasEstablishedCall) {
                if (snapshot != null) {
                    if (!isAutoCallStatusApplied(state.selfUser)) {
                        applyMeetingStatus(userId, state.selfUser)
                    }
                    return
                }

                dataStore.saveAutoCallStatusSnapshot(
                    availabilityStatus = state.selfUser.availabilityStatus,
                    textStatus = state.selfUser.textStatus
                )
                applyMeetingStatus(userId, state.selfUser)
                return
            }

            if (snapshot != null) {
                if (restoreSnapshot(userId, state.selfUser, snapshot.availabilityStatus, snapshot.textStatus)) {
                    dataStore.clearAutoCallStatusSnapshot()
                }
            }
        }
    }

    private suspend fun applyMeetingStatus(userId: UserId, selfUser: SelfUser): Boolean {
        val statusText = buildTextStatus(
            emoji = QUICK_STATUS_IN_MEETING_EMOJI,
            message = context.getString(R.string.user_profile_quick_status_in_meeting)
        )
        val userScope = coreLogic.getSessionScope(userId).users
        var success = true

        if (selfUser.availabilityStatus != AUTO_CALL_AVAILABILITY) {
            userScope.updateSelfAvailabilityStatus(AUTO_CALL_AVAILABILITY)
        }

        if (selfUser.textStatus != statusText) {
            userScope.updateSelfTextStatus(statusText)
                .fold(
                    fnL = {
                        success = false
                        appLogger.e("Failed to update auto call text status for $userId: $it")
                    },
                    fnR = {}
                )
        }

        return success
    }

    private suspend fun restoreSnapshot(
        userId: UserId,
        selfUser: SelfUser,
        availabilityStatus: UserAvailabilityStatus,
        textStatus: String?
    ): Boolean {
        val userScope = coreLogic.getSessionScope(userId).users
        var success = true

        if (selfUser.availabilityStatus != availabilityStatus) {
            userScope.updateSelfAvailabilityStatus(availabilityStatus)
        }

        if (selfUser.textStatus != textStatus) {
            userScope.updateSelfTextStatus(textStatus)
                .fold(
                    fnL = {
                        success = false
                        appLogger.e("Failed to restore text status after call for $userId: $it")
                    },
                    fnR = {}
                )
        }

        return success
    }

    private fun userMutex(userId: UserId): Mutex = userMutexes.getOrPut(userId) { Mutex() }

    private fun isAutoCallStatusApplied(selfUser: SelfUser): Boolean =
        selfUser.availabilityStatus == AUTO_CALL_AVAILABILITY &&
            selfUser.textStatus == buildTextStatus(
                emoji = QUICK_STATUS_IN_MEETING_EMOJI,
                message = context.getString(R.string.user_profile_quick_status_in_meeting)
            )

    private data class AccountCallStatusState(
        val isTeamMember: Boolean,
        val establishedCalls: List<Call>,
        val selfUser: SelfUser,
    )

    companion object {
        private val AUTO_CALL_AVAILABILITY = UserAvailabilityStatus.BUSY
        private const val QUICK_STATUS_IN_MEETING_EMOJI = "\uD83C\uDFA7"
    }
}
