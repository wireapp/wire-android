/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature

import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Evaluates the startup warning once per main activity task within this process. */
@SingleIn(AppScope::class)
class StartupAccountLimitGate @Inject constructor() {
    private val evaluatedTaskIds = mutableSetOf<Int>()

    @Synchronized
    fun shouldShow(accounts: List<AccountInfo>, currentUserId: UserId?, maxAccounts: Int, taskId: Int): Boolean {
        if (!evaluatedTaskIds.add(taskId)) return false
        val validAccounts = accounts.filterIsInstance<AccountInfo.Valid>()
        return currentUserId != null && validAccounts.any { it.userId == currentUserId } && validAccounts.size > maxAccounts
    }
}
