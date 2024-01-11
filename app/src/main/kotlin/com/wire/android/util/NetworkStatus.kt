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

package com.wire.android.util

/**
 * Information about the current network status by our best guess. This information
 * isn't guaranteed to be 100% accurate.
 */
data class NetworkStatus(val isOnVpn: Boolean, val isMetered: Boolean) {
  override fun toString(): String {
    return "[isOnVpn: $isOnVpn, isMetered: $isMetered]"
  }
}
