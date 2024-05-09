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
@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui.common.textfield

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.text.isDigitsOnly

@OptIn(ExperimentalFoundationApi::class)
class MaxLengthDigitsFilter(private val maxLength: Int) : InputTransformation {
    override val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    init {
        require(maxLength >= 0) { "maxLength must be at least zero, was $maxLength" }
    }
    override fun transformInput(originalValue: TextFieldCharSequence, valueWithChanges: TextFieldBuffer) {
        val newLength = valueWithChanges.length
        if (newLength > maxLength || !valueWithChanges.asCharSequence().isDigitsOnly()) {
            valueWithChanges.revertAllChanges()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Stable
fun InputTransformation.maxLengthDigits(maxLength: Int): InputTransformation = this.then(MaxLengthDigitsFilter(maxLength))
