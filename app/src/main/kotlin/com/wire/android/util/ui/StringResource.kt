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

sealed class LocalizedStringResource {

    abstract fun getString(context: Context): String
    data class StringResource(@StringRes val id: Int) : LocalizedStringResource() {
        override fun getString(context: Context): String = context.getString(id)
    }
    data class PluralResource(@PluralsRes val id: Int, val quantity: Int, val formatArgs: Array<Any>) : LocalizedStringResource() {
        override fun getString(context: Context): String = context.resources.getQuantityString(id, quantity, *formatArgs)
    }
}
