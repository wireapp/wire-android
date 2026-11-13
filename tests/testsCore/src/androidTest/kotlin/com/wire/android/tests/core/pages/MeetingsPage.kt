/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.tests.core.pages

import android.graphics.Rect
import android.os.Build
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.wire.android.tests.support.UiAutomatorSetup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import service.models.Meeting
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.toBySelector
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** UIAutomator assertions against rendered meeting cards, including off-screen paged entries. */
@Suppress("TooManyFunctions")
class MeetingsPage(private val device: UiDevice) {
    private val mainMenuButton = UiSelectorParams(description = "Main navigation")
    private val germanMainMenuButton = UiSelectorParams(description = "Hauptnavigation")
    private val meetingsHeading = UiSelectorParams(text = "Meetings")
    private val meetingInviteNotification = By.text(Regex(".*(ML-.*|invited (you )?to a meeting|zu einem Meeting).*").toPattern())
        .pkg("com.android.systemui")
    private val remindersDialogTitle = UiSelectorParams(text = "Enable precise meeting reminders")
    private val samsungSoftwareUpdatePrompt = By.pkg("com.wssyncmldm").textStartsWith("Update ready to install")
    private val cancelButton = UiSelectorParams(text = "Cancel")
    private val germanCancelButton = UiSelectorParams(text = "Abbrechen")
    private val emptyTitle = UiSelectorParams(text = "No upcoming meetings yet", timeout = 30.seconds)
    private val emptyDescription = UiSelectorParams(text = "Start a meeting with your team members.")
    private val moreMenuButton = UiSelectorParams(description = "Meeting more menu icon")
    private val manualLoadButton = UiSelectorParams(textMatches = "(?i)^(Show More|Load More)$")
    private val participantAvatar = UiSelectorParams(resourceId = "User avatar")
    private val dailyRecurrence = UiSelectorParams(description = "Daily")
    private val germanDailyRecurrence = UiSelectorParams(description = "Täglich")
    private val anyText = UiSelectorParams(textMatches = ".+")
    private val listEntry = UiSelectorParams(
        textMatches = "^(ML-.*|(Today|Tomorrow|Yesterday|Heute|Morgen|Gestern) \\(.*\\)|" +
            "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|Montag|Dienstag|Mittwoch|Donnerstag|Freitag|Samstag|Sonntag), .*)$"
    )
    private fun meetingTitle(title: String) = By.text(title).pkg(UiAutomatorSetup.appPackage)
    private fun dateHeader(text: String) = UiSelectorParams(text = text)

    fun open(locale: Locale = Locale.getDefault()) {
        val navigationButton = if (locale.language == "de") germanMainMenuButton else mainMenuButton
        UiWaitUtils.waitElement(navigationButton, timeout = 30.seconds)
        // An invitation can arrive just after its meeting is created.
        device.wait(Until.hasObject(meetingInviteNotification), UiWaitUtils.STABLE_TIMEOUT.inWholeMilliseconds)
        UiWaitUtils.waitUntilGoneOrThrow(
            meetingInviteNotification,
            errorMessage = "Meeting invite notification is still covering the navigation button"
        )
        assertTrue("Main navigation button is not clickable", UiWaitUtils.clickWhenClickable(navigationButton, timeout = 30.seconds))
        UiWaitUtils.waitElement(meetingsHeading, timeout = 30.seconds).click()
        // The app asks for exact-alarm access the first time this screen opens.
        if (UiWaitUtils.retryUntilTimeout(5.seconds) { device.hasObject(remindersDialogTitle.toBySelector()) }) {
            UiWaitUtils.waitElement(if (locale.language == "de") germanCancelButton else cancelButton).click()
            UiWaitUtils.waitUntilGoneOrThrow(
                remindersDialogTitle.toBySelector(),
                timeout = 5.seconds,
                errorMessage = "Precise meeting reminders dialog was not dismissed"
            )
        }
        UiWaitUtils.waitElement(meetingsHeading, timeout = 30.seconds)
    }

    fun assertEmpty() {
        UiWaitUtils.waitElement(emptyTitle)
        UiWaitUtils.waitElement(emptyDescription)
        assertFalse("Empty list contains meeting cards", device.hasObject(moreMenuButton.toBySelector()))
    }

    fun dismissSamsungSoftwareUpdatePromptIfShown() {
        if (!Build.MANUFACTURER.equals("samsung", ignoreCase = true)) return
        // Changing the clock can bring Samsung's pending update screen to the foreground.
        if (device.wait(Until.hasObject(samsungSoftwareUpdatePrompt), UiWaitUtils.SHORT_TIMEOUT.inWholeMilliseconds)) {
            device.executeShellCommand("input keyevent KEYCODE_BACK")
            UiWaitUtils.waitUntilGoneOrThrow(
                samsungSoftwareUpdatePrompt,
                timeout = UiWaitUtils.SHORT_TIMEOUT,
                errorMessage = "Samsung software update screen did not close"
            )
        }
    }

    fun assertMeeting(
        meeting: Meeting,
        zone: ZoneId,
        locale: Locale = Locale.getDefault(),
        participantCount: Int? = null,
    ) {
        val expectedTime = timeRange(meeting, zone, locale)
        var title: VisibleEntry? = null
        val meetingVisible = UiWaitUtils.retryUntilTimeout(30.seconds) {
            try {
                val node = device.findObjects(meetingTitle(meeting.title)).singleOrNull()
                    ?: return@retryUntilTimeout false
                val entry = VisibleEntry(node.text, node.visibleBounds)
                if (expectedTime !in rowTexts(entry)) return@retryUntilTimeout false
                title = entry
                true
            } catch (_: StaleObjectException) {
                false
            }
        }
        if (!meetingVisible) {
            throw AssertionError(
                "Meeting '${meeting.title}' did not display its latest information: " +
                    "expected '$expectedTime' for $locale; actual ${currentRowTexts(meeting.title)}"
            )
        }
        assertCardDetails(requireNotNull(title), meeting, zone, locale, participantCount)
    }

    fun assertGone(title: String, timeout: Duration = 30.seconds) {
        UiWaitUtils.waitUntilGoneOrThrow(
            meetingTitle(title),
            timeout = timeout,
            errorMessage = "Stale meeting '$title' is still displayed"
        )
    }

    fun assertNotVisible(title: String) {
        assertFalse("Meeting '$title' should require scrolling", device.hasObject(meetingTitle(title)))
    }

    fun assertNoCountdown(title: String) {
        val node = device.wait(Until.findObject(meetingTitle(title)), UiWaitUtils.DEFAULT_TIMEOUT.inWholeMilliseconds)
            ?: throw AssertionError("Meeting '$title' is not visible")
        assertFalse("Unexpected starting-soon countdown for $title", rowTexts(node).any { it.startsWith("Starting in ") })
    }

    fun assertHasCountdown(title: String, timeout: Duration = 10.seconds) {
        assertTrue(
            "Expected a starting-soon countdown for $title",
            UiWaitUtils.retryUntilTimeout(timeout) {
                currentRowTexts(title)?.any { it.startsWith("Starting in ") } == true
            }
        )
    }

    fun awaitCountdown(title: String, countdown: String, timeout: Duration) {
        val configuration = Configurator.getInstance()
        val originalIdleTimeout = configuration.waitForIdleTimeout
        try {
            // Waiting for UI idle can skip a one-second countdown value on an actively updating screen.
            configuration.waitForIdleTimeout = 0
            assertTrue(
                "Expected '$countdown' for $title",
                UiWaitUtils.retryUntilTimeout(timeout, 100.milliseconds) {
                    currentRowTexts(title)?.contains("Starting in $countdown") == true
                }
            )
        } finally {
            configuration.waitForIdleTimeout = originalIdleTimeout
        }
    }

    fun awaitJoin(title: String, timeout: Duration = 30.seconds) {
        assertTrue(
            "Meeting $title did not automatically transition to Join",
            UiWaitUtils.retryUntilTimeout(timeout) {
                currentRowTexts(title)?.let { texts ->
                    "Join" in texts && texts.none { text -> text.startsWith("Starting in ") }
                } == true
            }
        )
        assertEquals("Duplicate live meeting", 1, device.findObjects(meetingTitle(title)).size)
    }

    /**
     * Traverse overlapping viewports. Each viewport must be a contiguous slice of the expected header/card sequence.
     * This checks grouping, ordering, empty sections, duplicates, missing entries and unexpected jumps to the top.
     */
    fun assertCompleteList(
        meetings: List<Meeting>,
        zone: ZoneId,
        today: LocalDate,
        locale: Locale = Locale.getDefault(),
        participants: Map<String, Int> = emptyMap(),
    ) {
        require(meetings.isNotEmpty() && meetings.map { it.title }.distinct().size == meetings.size)
        val expected = expectedEntries(meetings, zone, today, locale)
        val observed = mutableSetOf<Int>()
        val checkedCards = mutableSetOf<String>()
        var previousFirst = -1
        var lastReached = -1
        var atEnd = false
        repeat(MAX_SCROLLS) {
            val entries = settledEntries(expected)
            assertTrue("No meeting entries visible while scrolling", entries.isNotEmpty())
            val indices = entries.map { entry ->
                expected.indexOf(entry.text).also { index ->
                    assertTrue("Unexpected or incorrectly formatted entry: ${entry.text}; expected $expected", index >= 0)
                }
            }
            if (previousFirst == -1) assertEquals("List did not open at its beginning", 0, indices.first())
            assertTrue("List jumped backwards while appending pages", indices.first() >= previousFirst)
            assertTrue("Scrolling skipped entries after index $lastReached", indices.first() <= lastReached + 1)
            indices.zipWithNext().forEach { (before, after) ->
                assertEquals("Duplicate, missing or incorrectly grouped card in ${entries.map { it.text }}", before + 1, after)
            }
            observed.addAll(indices)
            previousFirst = indices.first()
            lastReached = maxOf(lastReached, indices.last())
            assertFalse("Pagination requires a manual load button", device.hasObject(manualLoadButton.toBySelector()))
            entries.forEachIndexed { index, node ->
                val meeting = meetings.find { it.title == node.text }
                // A following entry proves this card is fully in view. The final viewport covers the last card.
                if (meeting != null && (index < entries.lastIndex || atEnd)) {
                    assertCardDetails(node, meeting, zone, locale, participants[meeting.title])
                    checkedCards.add(meeting.title)
                }
            }
            if (atEnd) {
                assertEquals("Meetings were skipped or missing", expected.indices.toSet(), observed)
                assertEquals("Not all meeting information was checked", meetings.map { it.title }.toSet(), checkedCards)
                return
            }
            atEnd = scrollToNextViewport(entries.map { it.text }, expected.last())
        }
        throw AssertionError("Meetings list did not reach its end in $MAX_SCROLLS scrolls")
    }

    fun returnToTopAndAssertFirst(title: String) {
        repeat(MAX_SCROLLS) {
            val before = visibleEntries().map { it.text }
            if (before.firstOrNull { it.startsWith("ML-") } == title) {
                // Reveal the section header too if the first card is only partially visible.
                list()?.scroll(Direction.UP, 1f)
                assertEquals("Previously loaded meeting disappeared", title, visibleEntries().first { it.text.startsWith("ML-") }.text)
                return
            }
            assertTrue(
                "Could not scroll back to previously loaded meeting '$title'; visible entries: $before",
                UiWaitUtils.retryUntilTimeout(10.seconds) {
                    list()?.scroll(Direction.UP, 0.8f)
                    visibleEntries().map { it.text } != before
                }
            )
        }
        throw AssertionError("Could not return to the beginning of the Meetings list")
    }

    fun assertHeader(meeting: Meeting, zone: ZoneId, today: LocalDate, locale: Locale = Locale.getDefault()) {
        val expected = header(meeting.start.atZone(zone).toLocalDate(), today, locale)
        assertTrue(
            "Expected header '$expected'; visible entries: ${visibleEntries().map { it.text }}",
            UiWaitUtils.retryUntilTimeout(10.seconds) { device.hasObject(dateHeader(expected).toBySelector()) }
        )
    }

    private fun assertCardDetails(
        node: VisibleEntry,
        meeting: Meeting,
        zone: ZoneId,
        locale: Locale,
        participantCount: Int?,
    ) {
        val texts = rowTexts(node)
        assertTrue("Wrong start/end time for ${meeting.title}: $texts", timeRange(meeting, zone, locale) in texts)
        val recurrence = if (locale.language == "de") germanDailyRecurrence else dailyRecurrence
        assertEquals("Wrong recurrence marker for ${meeting.title}", meeting.recurring, rowNodes(recurrence, node).isNotEmpty())
        participantCount?.let { count ->
            val avatars = rowNodes(participantAvatar, node)
            assertEquals("Wrong participant avatar count in ${meeting.title}", minOf(count, 5), avatars.size)
            if (count > 5) assertTrue("Missing participant overflow count", "+${count - 5}" in texts)
        }
    }

    private fun currentRowTexts(title: String): List<String>? = try {
        device.findObject(meetingTitle(title))?.let { rowTexts(it) }
    } catch (_: StaleObjectException) {
        null
    }

    private fun rowTexts(title: UiObject2): List<String> = rowTexts(VisibleEntry(title.text, title.visibleBounds))

    private fun rowTexts(title: VisibleEntry): List<String> = readFreshNodes { rowNodes(anyText, title).map { it.text } }

    private fun rowNodes(selector: UiSelectorParams, title: VisibleEntry): List<UiObject2> {
        val top = title.visibleBounds.top
        val bottom = visibleEntries().filter { it.visibleBounds.top > top }.minOfOrNull { it.visibleBounds.top }
            ?: device.displayHeight
        return device.findObjects(selector.toBySelector().pkg(UiAutomatorSetup.appPackage)).filter {
            it.visibleBounds.top >= top && it.visibleBounds.bottom <= bottom
        }
    }

    private data class VisibleEntry(val text: String, val visibleBounds: Rect)

    private fun visibleEntries(): List<VisibleEntry> = readFreshNodes {
        device.findObjects(listEntry.toBySelector().pkg(UiAutomatorSetup.appPackage))
            .map { VisibleEntry(it.text, it.visibleBounds) }
            .filter { !it.visibleBounds.isEmpty }
            .sortedBy { it.visibleBounds.top }
    }

    private fun settledEntries(expected: List<String>): List<VisibleEntry> {
        var previous = emptyList<VisibleEntry>()
        var latest = emptyList<VisibleEntry>()
        val settled = UiWaitUtils.retryUntilTimeout(5.seconds) {
            latest = visibleEntries()
            val indices = latest.map { expected.indexOf(it.text) }
            // Paging and animateItem can briefly expose a partial accessibility tree without stale-node errors.
            // Require two matching snapshots, including bounds, before checking cards or scrolling again.
            val complete = indices.isNotEmpty() && indices.all { it >= 0 } &&
                indices.zipWithNext().all { (before, after) -> after == before + 1 }
            val stable = complete && latest == previous
            previous = latest
            stable
        }
        assertTrue(
            "Meeting viewport did not settle into a contiguous sequence; visible entries: ${latest.map { it.text }}",
            settled
        )
        return latest
    }

    private fun <T : Any> readFreshNodes(read: () -> T): T {
        var snapshot: T? = null
        assertTrue(
            "Meeting nodes kept changing while reading the viewport",
            UiWaitUtils.retryUntilTimeout(5.seconds) {
                try {
                    snapshot = read()
                    true
                } catch (_: StaleObjectException) {
                    false
                }
            }
        )
        return requireNotNull(snapshot)
    }

    private fun list(): UiObject2? = device.findObjects(By.scrollable(true))
        .lastOrNull { it.hasObject(moreMenuButton.toBySelector()) }

    private fun scrollToNextViewport(before: List<String>, finalEntry: String): Boolean {
        if (list()?.scroll(Direction.DOWN, 0.45f) == true || visibleEntries().map { it.text } != before) return false
        val atEnd = before.last() == finalEntry
        if (!atEnd) {
            assertTrue(
                "Additional meetings did not load after scrolling",
                UiWaitUtils.retryUntilTimeout(30.seconds) {
                    visibleEntries().map { it.text } != before || list()?.scroll(Direction.DOWN, 0.45f) == true ||
                        visibleEntries().map { it.text } != before
                }
            )
        }
        return atEnd
    }

    private fun expectedEntries(meetings: List<Meeting>, zone: ZoneId, today: LocalDate, locale: Locale): List<String> {
        val entries = mutableListOf<String>()
        var day: LocalDate? = null
        meetings.forEach { meeting ->
            val date = meeting.start.atZone(zone).toLocalDate()
            if (date != day) entries.add(header(date, today, locale))
            entries.add(meeting.title)
            day = date
        }
        return entries
    }

    private fun header(date: LocalDate, today: LocalDate, locale: Locale): String {
        // Meetings currently use textual headers; they do not render a numeric date field.
        val dateText = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale))
        val relative = when (date) {
            today -> if (locale.language == "de") "Heute" else "Today"
            today.plusDays(1) -> if (locale.language == "de") "Morgen" else "Tomorrow"
            else -> null
        }
        return relative?.let { "$it ($dateText)" } ?: dateText
    }

    private fun timeRange(meeting: Meeting, zone: ZoneId, locale: Locale): String {
        // The localized formatter returns a malformed en-DE time pattern on Android 12.
        val format = if (locale.toLanguageTag() == "en-DE") {
            DateTimeFormatter.ofPattern("HH:mm", locale)
        } else {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        }.withZone(zone)
        return "${format.format(meeting.start)} - ${format.format(meeting.end)}"
    }

    private companion object {
        const val MAX_SCROLLS = 250
    }
}
