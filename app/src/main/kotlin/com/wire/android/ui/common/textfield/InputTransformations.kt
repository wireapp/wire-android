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

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Stable
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.maxTextLength
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.text.isDigitsOnly
import java.util.regex.Pattern

class MaxLengthDigitsFilter(private val maxLength: Int) : InputTransformation {
    override val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    init {
        require(maxLength >= 0) { "maxLength must be at least zero, was $maxLength" }
    }
    override fun SemanticsPropertyReceiver.applySemantics() {
        maxTextLength = maxLength
    }
    override fun TextFieldBuffer.transformInput() {
        if (length > maxLength || !asCharSequence().isDigitsOnly()) {
            revertAllChanges()
        }
    }
}

@Stable
fun InputTransformation.maxLengthDigits(maxLength: Int): InputTransformation = this.then(MaxLengthDigitsFilter(maxLength))

class MaxLengthFilterWithCallback(private val maxLength: Int, private val onIncorrectChangesFound: () -> Unit) : InputTransformation {
    init {
        require(maxLength >= 0) { "maxLength must be at least zero, was $maxLength" }
    }
    override fun SemanticsPropertyReceiver.applySemantics() {
        maxTextLength = maxLength
    }
    override fun TextFieldBuffer.transformInput() {
        if (length > maxLength) {
            revertAllChanges()
            onIncorrectChangesFound()
        }
    }
}

@Stable
fun InputTransformation.maxLengthWithCallback(maxLength: Int, onIncorrectChangesFound: () -> Unit): InputTransformation =
    this.then(MaxLengthFilterWithCallback(maxLength, onIncorrectChangesFound))

class PatternFilterWithCallback(private val pattern: Pattern, private val onIncorrectChangesFound: () -> Unit) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (!pattern.matcher(asCharSequence()).matches()) {
            revertAllChanges()
            onIncorrectChangesFound()
        }
    }
}

@Stable
fun InputTransformation.patternWithCallback(pattern: Pattern, onIncorrectChangesFound: () -> Unit): InputTransformation =
    this.then(PatternFilterWithCallback(pattern, onIncorrectChangesFound))
