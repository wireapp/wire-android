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
package com.wire.android.ui.home.newconversation.channelhistory

import android.os.Parcelable
import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import kotlinx.parcelize.Parcelize

sealed interface ChannelHistoryType : Parcelable {

    @Parcelize
    data object Off : ChannelHistoryType

    sealed interface On : ChannelHistoryType {

        @Parcelize
        data object Unlimited : On

        @Parcelize
        data class Specific(val amount: Int, val type: AmountType) : On {

            @Parcelize
            enum class AmountType(@PluralsRes val nameResId: Int, @PluralsRes val nameWithAmountResId: Int) : Parcelable {
                Days(R.plurals.days_label, R.plurals.days_long_label),
                Weeks(R.plurals.weeks_label, R.plurals.weeks_long_label),
                Months(R.plurals.months_label, R.plurals.months_long_label)
            }
        }
    }
}

@Suppress("MagicNumber")
val defaultHistoryTypes: List<ChannelHistoryType> = listOf(
    ChannelHistoryType.Off,
    ChannelHistoryType.On.Specific(1, ChannelHistoryType.On.Specific.AmountType.Days),
    ChannelHistoryType.On.Specific(1, ChannelHistoryType.On.Specific.AmountType.Weeks),
    ChannelHistoryType.On.Specific(4, ChannelHistoryType.On.Specific.AmountType.Weeks),
    ChannelHistoryType.On.Unlimited,
)

@Suppress("MagicNumber")
val defaultFreemiumHistoryTypes: List<ChannelHistoryType> = listOf(
    ChannelHistoryType.Off,
    ChannelHistoryType.On.Specific(1, ChannelHistoryType.On.Specific.AmountType.Days),
)

fun ChannelHistoryType.isCustom(): Boolean = !defaultHistoryTypes.contains(this)

@Composable
fun ChannelHistoryType.name(useAmountForCustom: Boolean): String = when (this) {
    is ChannelHistoryType.Off -> stringResource(R.string.channel_history_off)
    is ChannelHistoryType.On.Unlimited -> stringResource(R.string.channel_history_unlimited)
    is ChannelHistoryType.On.Specific -> when {
        this.isCustom() && !useAmountForCustom -> stringResource(R.string.channel_history_custom)
        else -> this.amountAsString()
    }
}

@Composable
fun ChannelHistoryType.On.Specific.amountAsString(): String = pluralStringResource(type.nameWithAmountResId, amount, amount)
