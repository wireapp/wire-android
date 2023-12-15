/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.util.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

sealed interface LocalizedStringResource {

    fun getString(context: Context): String
    data class StringResource(@StringRes val id: Int) : LocalizedStringResource {
        override fun getString(context: Context): String = context.getString(id)
    }

    data class PluralResource(@PluralsRes val id: Int, val quantity: Int, val formatArgs: Array<Any>) : LocalizedStringResource {
        override fun getString(context: Context): String = context.resources.getQuantityString(id, quantity, formatArgs)
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PluralResource

            if (id != other.id) return false
            if (quantity != other.quantity) return false
            if (!formatArgs.contentEquals(other.formatArgs)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + quantity
            result = 31 * result + formatArgs.contentHashCode()
            return result
        }
    }
}
