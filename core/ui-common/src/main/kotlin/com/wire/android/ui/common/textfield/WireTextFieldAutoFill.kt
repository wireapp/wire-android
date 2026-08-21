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
package com.wire.android.ui.common.textfield

import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.contentType
import io.github.esentsov.PackagePrivate

@PackagePrivate
internal fun Modifier.applyAutofill(type: WireAutoFillType): Modifier =
    type.contentType?.let { contentType(it) } ?: this

enum class WireAutoFillType(val contentType: ContentType?) {
    None(null),
    Login(ContentType.EmailAddress + ContentType.Username),
    Password(ContentType.Password),
}
