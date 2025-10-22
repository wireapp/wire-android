/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversationslist.model

import androidx.annotation.StringRes
import com.wire.android.ui.common.R

enum class Membership(@StringRes val stringResourceId: Int) {
    Guest(R.string.label_membership_guest),
    Federated(R.string.label_federated_membership),
    External(R.string.label_membership_external),
    Service(R.string.label_membership_service),
    Owner(-1),
    Admin(-1),
    Standard(-1),
    None(-1)
}

fun Membership.hasLabel(): Boolean = stringResourceId != -1

fun Membership.allowsRoleEdition() = this != Membership.Federated && this != Membership.Service
