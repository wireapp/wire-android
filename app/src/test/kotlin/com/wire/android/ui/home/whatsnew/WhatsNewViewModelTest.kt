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
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import com.wire.android.R
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.util.toMediumOnlyDateTime
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.text.SimpleDateFormat
import java.util.Date

@ExtendWith(CoroutineTestExtension::class)
class WhatsNewViewModelTest {

    @Test
    fun `given url is not blank, when fetching release notes, then execute getRssChannel`() = runTest {
        val url = "url"
        val (arrangement, viewModel) = Arrangement()
            .withFeedResult(testRssChannel)
            .withFeedUrl(url)
            .arrange()

        advanceUntilIdle()

        assertEquals(testReleaseNotes, viewModel.state.releaseNotesItems)
        assertEquals(false, viewModel.state.isLoading)
        coVerify(exactly = 1) {
            arrangement.rssParser.getRssChannel(url)
        }
    }

    @Test
    fun `given url is blank, when fetching release notes, then do not execute getRssChannel`() = runTest {
        val url = ""
        val (arrangement, viewModel) = Arrangement()
            .withFeedUrl(url)
            .arrange()

        advanceUntilIdle()

        assertEquals(emptyList<ReleaseNotesItem>(), viewModel.state.releaseNotesItems)
        assertEquals(false, viewModel.state.isLoading)
        coVerify(exactly = 0) {
            arrangement.rssParser.getRssChannel(url)
        }
    }

    inner class Arrangement {

        @MockK
        lateinit var context: Context

        @MockK
        lateinit var rssParser: RssParser

        val viewModel by lazy {
            WhatsNewViewModel(context)
        }

        fun withFeedUrl(feedUrl: String) = apply {
            coEvery { context.resources.getString(R.string.url_android_release_notes_feed) } returns feedUrl
        }

        fun withFeedResult(rssChannel: RssChannel) = apply {
            coEvery { rssParser.getRssChannel(any()) } returns rssChannel
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkStatic(::RssParser)
            coEvery { RssParser() } returns rssParser
            mockkStatic(Date::toMediumOnlyDateTime)
            coEvery { any<Date>().toMediumOnlyDateTime() } answers {
                SimpleDateFormat("dd MMM yyyy").format(firstArg())
            }
        }

        fun arrange() = this to viewModel
    }

    private val testRssItem = RssItem(
        guid = "guid",
        title = "itemTitle",
        author = "author",
        link = "link",
        pubDate = "Mon, 01 Jan 2024 00:00:00",
        description = null,
        content = null,
        image = null,
        audio = null,
        video = null,
        sourceName = null,
        sourceUrl = null,
        categories = emptyList(),
        itunesItemData = null,
        commentsUrl = null,
    )
    private val testRssChannel: RssChannel = RssChannel(
        title = "title",
        items = listOf(testRssItem),
        link = null,
        description = null,
        image = null,
        lastBuildDate = null,
        updatePeriod = null,
        itunesChannelData = null,
    )
    private val testReleaseNoteItem = ReleaseNotesItem(
        id = "guid",
        title = "itemTitle",
        link = "link",
        publishDate = "01 Jan 2024",
    )
    private val testReleaseNotes: List<ReleaseNotesItem> = listOf(testReleaseNoteItem)
}
