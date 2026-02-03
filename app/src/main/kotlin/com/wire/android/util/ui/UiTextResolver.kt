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

package com.wire.android.util.ui

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

interface UiTextResolver {

    // Resolves UIText outside of Compose without holding Activity/Fragment context.
    fun resolve(text: UIText): String

    // Tag used to invalidate precomputed markdown when the locale changes.
    fun localeTag(): String
}

class AndroidUiTextResolver @Inject constructor(
    @ApplicationContext private val context: Context
) : UiTextResolver {

    override fun resolve(text: UIText): String = text.asString(context.resources)

    override fun localeTag(): String {
        val locales = context.resources.configuration.locales
        val locale = if (locales.isEmpty) Locale.getDefault() else locales[0]
        return locale.toLanguageTag()
    }
}
