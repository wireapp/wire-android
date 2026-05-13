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
package com.wire.android.ui.home.whatsnew

import android.content.Context
import com.wire.android.R
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface ReleaseNotesFeedUrlProvider {
    val feedUrl: String
}

class AndroidReleaseNotesFeedUrlProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : ReleaseNotesFeedUrlProvider {
    override val feedUrl: String
        get() = context.resources.getString(R.string.url_android_release_notes_feed)
}

@Module
@InstallIn(ViewModelComponent::class)
interface ReleaseNotesFeedUrlProviderModule {
    @Binds
    fun bindReleaseNotesFeedUrlProvider(
        provider: AndroidReleaseNotesFeedUrlProvider
    ): ReleaseNotesFeedUrlProvider
}
