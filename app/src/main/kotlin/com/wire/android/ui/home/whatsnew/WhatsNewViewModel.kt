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
package com.wire.android.ui.home.whatsnew

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.rssparser.RssParser
import com.wire.android.R
import com.wire.android.util.toMediumOnlyDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WhatsNewViewModel @Inject constructor(context: Context) : ViewModel() {
    private val rssParser = RssParser()
    private val publishDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH)

    var state by mutableStateOf(WhatsNewState(isLoading = true))
        private set

    init {
        @Suppress("TooGenericExceptionCaught")
        viewModelScope.launch {
            val feedUrl = context.resources.getString(R.string.url_android_release_notes_feed)
            val items = try {
                if (feedUrl.isNotBlank()) {
                    rssParser.getRssChannel(feedUrl).items
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }

            state = state.copy(
                isLoading = false,
                releaseNotesItems = items
                    .map { item ->
                        ReleaseNotesItem(
                            id = item.guid.orEmpty(),
                            title = item.title.orEmpty(),
                            link = item.link.orEmpty(),
                            publishDate = item.pubDate?.let { publishDateFormat.parse(it)?.toMediumOnlyDateTime() }.orEmpty(),
                        )
                    }
                    .filter {
                        it.title.isNotBlank() && it.link.isNotBlank() && it.publishDate.isNotBlank()
                    }
            )
        }
    }
}
