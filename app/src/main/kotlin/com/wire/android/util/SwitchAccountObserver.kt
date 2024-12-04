/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.util

import com.wire.android.feature.SwitchAccountActions
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SwitchAccountObserver @Inject constructor() : SwitchAccountActions {
    private val lock = Object()
    private val items = mutableListOf<SwitchAccountActions>()

    fun register(actions: SwitchAccountActions) {
        synchronized(lock) {
            items.add(actions)
        }
    }

    fun unregister(actions: SwitchAccountActions) {
        synchronized(lock) {
            items.remove(actions)
        }
    }

    override fun switchedToAnotherAccount() {
        synchronized(lock) {
            items.forEach {
                it.switchedToAnotherAccount()
            }
        }
    }

    override fun noOtherAccountToSwitch() {
        synchronized(lock) {
            items.forEach {
                it.noOtherAccountToSwitch()
            }
        }
    }
}
