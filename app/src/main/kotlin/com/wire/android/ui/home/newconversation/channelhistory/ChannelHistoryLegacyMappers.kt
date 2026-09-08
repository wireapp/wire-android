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

package com.wire.android.ui.home.newconversation.channelhistory

internal fun ChannelHistoryType.toSelection(): ChannelHistorySelection = when (this) {
    ChannelHistoryType.Off -> ChannelHistorySelection.Off
    ChannelHistoryType.On.Unlimited -> ChannelHistorySelection.Unlimited
    is ChannelHistoryType.On.Specific -> ChannelHistorySelection.Specific(
        amount = amount,
        unit = type.toSelection(),
    )
}

internal fun ChannelHistoryType.On.Specific.toSelection(): ChannelHistorySelection.Specific =
    ChannelHistorySelection.Specific(
        amount = amount,
        unit = type.toSelection(),
    )

internal fun ChannelHistorySelection.toLegacy(): ChannelHistoryType = when (this) {
    ChannelHistorySelection.Off -> ChannelHistoryType.Off
    ChannelHistorySelection.Unlimited -> ChannelHistoryType.On.Unlimited
    is ChannelHistorySelection.Specific -> ChannelHistoryType.On.Specific(
        amount = amount,
        type = unit.toLegacy(),
    )
}

private fun ChannelHistoryType.On.Specific.AmountType.toSelection() =
    when (this) {
        ChannelHistoryType.On.Specific.AmountType.Days -> ChannelHistorySelection.AmountUnit.DAYS
        ChannelHistoryType.On.Specific.AmountType.Weeks -> ChannelHistorySelection.AmountUnit.WEEKS
        ChannelHistoryType.On.Specific.AmountType.Months -> ChannelHistorySelection.AmountUnit.MONTHS
    }

private fun ChannelHistorySelection.AmountUnit.toLegacy() =
    when (this) {
        ChannelHistorySelection.AmountUnit.DAYS -> ChannelHistoryType.On.Specific.AmountType.Days
        ChannelHistorySelection.AmountUnit.WEEKS -> ChannelHistoryType.On.Specific.AmountType.Weeks
        ChannelHistorySelection.AmountUnit.MONTHS -> ChannelHistoryType.On.Specific.AmountType.Months
    }
