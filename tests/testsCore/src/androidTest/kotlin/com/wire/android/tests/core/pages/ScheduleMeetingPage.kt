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

import android.text.format.DateFormat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.toBySelector
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** Operates the real Compose form and Material date/time pickers through accessibility. */
@Suppress("TooManyFunctions", "MagicNumber")
class ScheduleMeetingPage(private val device: UiDevice) {
    fun open(locale: Locale = Locale.US) {
        description(if (locale.language == "de") "Meeting starten oder planen" else "Start or schedule a new Wire Meeting").click()
        text(if (locale.language == "de") "Meeting planen" else "Schedule a Meeting").click()
        description(if (locale.language == "de") "Ansicht neues Meeting schließen" else "Close new meeting view")
    }

    fun assertFields() {
        listOf("TITLE", "STARTS", "ENDS", "REPEATS", "PARTICIPANTS", "Schedule").forEach { text(it) }
        title()
        field("start date")
        field("start time")
        field("end date")
        field("end time")
        description("Select participants")
        repeatField()
        description("Close new meeting view")
    }

    fun enterTitle(value: String) {
        title().text = value
        closeKeyboardIfOpened()
    }

    fun assertTitle(expected: String) = assertEquals("Draft title changed", expected, title().text)

    fun assertScheduleEnabled(enabled: Boolean) {
        val matches = UiWaitUtils.retryUntilTimeout(5.seconds) {
            scheduleButton().isEnabled == enabled
        }
        assertTrue("Wrong Schedule enabled state: expected $enabled", matches)
    }

    fun schedule() {
        assertScheduleEnabled(true)
        scheduleButton().click()
    }

    fun awaitClosed(locale: Locale = Locale.US) {
        UiWaitUtils.waitUntilGoneOrThrow(
            UiSelectorParams(description = if (locale.language == "de") "Ansicht neues Meeting schließen" else "Close new meeting view")
                .toBySelector(),
            timeout = 30.seconds,
            errorMessage = "Schedule Meeting did not close"
        )
    }

    fun close(useBack: Boolean = false, locale: Locale = Locale.US) {
        closeKeyboardIfOpened()
        if (useBack) {
            device.pressBack()
        } else {
            description(if (locale.language == "de") "Ansicht neues Meeting schließen" else "Close new meeting view").click()
        }
    }

    fun assertDiscardConfirmation() {
        UiWaitUtils.waitElement(UiSelectorParams(textMatches = "(?i).*discard.*"))
        text("Cancel")
    }

    fun cancelDiscard() = text("Cancel").click()
    fun confirmDiscard() = text("Discard").click()

    fun assertSchedulingError() {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Could not schedule meeting", timeout = 60.seconds))
        text("Something went wrong while scheduling the meeting. Please try again.")
        text("OK").click()
        assertScheduleEnabled(true)
    }

    fun assertDateTimes(start: LocalDateTime, end: LocalDateTime, locale: Locale = Locale.US) {
        val dateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d", locale)
        val is24Hour = DateFormat.is24HourFormat(InstrumentationRegistry.getInstrumentation().targetContext)
        val timeFormat = if (is24Hour) {
            DateTimeFormatter.ofPattern("HH:mm", locale)
        } else {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        }
        assertEquals("Wrong start date", start.format(dateFormat), field("start date", locale).text)
        assertEquals("Wrong end date", end.format(dateFormat), field("end date", locale).text)
        assertEquals("Wrong start time", normalizedTime(start.format(timeFormat)), normalizedTime(field("start time", locale).text))
        assertEquals("Wrong end time", normalizedTime(end.format(timeFormat)), normalizedTime(field("end time", locale).text))
        assertFalse("End date must not be independently editable", field("end date", locale).isEnabled)
    }

    fun assertTimePreference(time: LocalTime, is24Hour: Boolean) {
        val expected = time.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.US))
        val actual = normalizedTime(field("start time").text)
        assertEquals("Time must follow Android 12/24-hour preference", expected, actual)
    }

    fun selectDate(date: LocalDate) {
        field("start date").click()
        // Material's calendar grid does not expose day nodes in this build; its input mode is accessible.
        description("Switch to text input mode").click()
        UiWaitUtils.waitElement(UiSelectorParams(className = "android.widget.EditText")).text =
            date.format(DateTimeFormatter.ofPattern("MMddyyyy", Locale.US))
        closeKeyboardIfOpened()
        text("OK").click()
    }

    fun selectStartTime(time: LocalTime) = selectTime("start time", time)
    fun selectEndTime(time: LocalTime) = selectTime("end time", time)

    fun selectRepeat(option: String) {
        repeatField().click()
        // The selected value also exists behind the popup; wait for another menu item to prove it has opened.
        val otherOption = if (option == "Daily") "Never" else "Daily"
        text(otherOption)
        text(option).click()
        UiWaitUtils.waitUntilGoneOrThrow(
            By.text(otherOption),
            timeout = 5.seconds,
            errorMessage = "Repeat menu did not close after selecting $option"
        )
        assertEquals("Wrong selected recurrence", option, repeatField().text)
    }

    fun openParticipants() {
        description("Select participants").click()
        text("Select participants")
    }

    fun searchParticipant(query: String) {
        description("Search people by name or username").click()
        UiWaitUtils.waitElement(UiSelectorParams(className = "android.widget.EditText")).text = query
        closeKeyboardIfOpened()
    }

    fun setParticipant(name: String, selected: Boolean) {
        var clicked = false
        val matches = UiWaitUtils.retryUntilTimeout(10.seconds) {
            try {
                val row = participantRow(name)
                if (participantIsSelected(row) == selected) return@retryUntilTimeout true
                if (!clicked) {
                    row.click()
                    clicked = true
                }
                false
            } catch (_: StaleObjectException) {
                false
            }
        }
        assertTrue("Selection not saved for $name", matches)
    }

    fun confirmParticipants() = text("Select").click()

    fun assertParticipants(names: List<String>) {
        val actual = description("Select participants").text.split(", ")
        assertEquals("Wrong final selection or duplicate participant", names.sorted(), actual.sorted())
    }

    private fun text(value: String) = UiWaitUtils.waitElement(UiSelectorParams(text = value))
    private fun description(value: String) = UiWaitUtils.waitElement(UiSelectorParams(description = value))

    // Title is the form's only editable input; its placeholder is exposed as a separate accessibility node.
    private fun title() = UiWaitUtils.waitElement(UiSelectorParams(className = "android.widget.EditText"))

    private fun field(name: String, locale: Locale = Locale.US): UiObject2 {
        val label = if (locale.language == "de") {
            when (name) {
                "start date" -> "Startdatum auswählen"
                "end date" -> "Enddatum auswählen"
                "start time" -> "Startzeit auswählen"
                "end time" -> "Endzeit auswählen"
                else -> error("Unknown field $name")
            }
        } else {
            "Select $name"
        }
        // Disabled fields still expose their value, but the shared interaction helper requires enabled nodes.
        return if (name == "end date") {
            requireNotNull(device.wait(Until.findObject(By.desc(label)), 5000)) { "End date field was not visible: $label" }
        } else {
            description(label)
        }
    }

    private fun selectTime(name: String, time: LocalTime) {
        field(name).click()
        // The functional tests use 24-hour mode, so the inner dial exposes hours 13..23 directly.
        UiWaitUtils.waitElement(UiSelectorParams(description = "${time.hour} hours")).click()
        // Selecting the current hour can leave the dial in hour mode. The minute header explicitly switches it.
        requireNotNull(device.wait(Until.findObject(By.desc(Pattern.compile("[0-9]+ minutes"))), 5000)) {
            "Minute selection header was not visible"
        }.click()
        if (time.minute % 5 == 0) {
            description("${time.minute} minutes").click()
        } else {
            // The dial snaps taps to five-minute marks; dragging permits the exact 23:59 boundary.
            val thirty = description("30 minutes").visibleCenter
            // The header and dial can both expose "0 minutes"; only the lower node belongs to the dial.
            val zero = device.findObjects(By.desc("0 minutes")).maxBy { it.visibleBounds.top }.visibleCenter
            val centerY = (zero.y + thirty.y) / 2.0
            val radius = (thirty.y - zero.y) / 2.0
            val angle = time.minute * Math.PI / 30
            val targetX = (zero.x + radius * sin(angle)).roundToInt()
            val targetY = (centerY - radius * cos(angle)).roundToInt()
            device.swipe(thirty.x, thirty.y, targetX, targetY, 60)
        }
        text("OK").click()
    }

    private fun participantRow(name: String): UiObject2 {
        val nameSelector = By.text(name).clazz(Pattern.compile("(?!android\\.widget\\.EditText$).*"))
        var participant: UiObject2? = null
        var expandedSelection = false
        val visible = UiWaitUtils.retryUntilTimeout(10.seconds) {
            participant = device.findObject(nameSelector)
            if (participant == null && !expandedSelection) {
                // Search results move asynchronously into a collapsed section after selection.
                UiWaitUtils.findElementOrNull(UiSelectorParams(textMatches = "SELECTED \\([0-9]+\\)"))?.let {
                    it.click()
                    expandedSelection = true
                }
            }
            participant != null
        }
        assertTrue("Participant row not visible: $name", visible)
        var node = requireNotNull(participant)
        while (!node.isClickable) {
            node = requireNotNull(node.parent) { "No selectable row for $name" }
        }
        return node
    }

    fun assertParticipantSelected(name: String) {
        val selected = UiWaitUtils.retryUntilTimeout(5.seconds) {
            try {
                participantIsSelected(participantRow(name))
            } catch (_: StaleObjectException) {
                false
            }
        }
        assertTrue("Previously selected participant was lost: $name", selected)
    }

    fun assertDateUsesAppLocale(date: LocalDate, locale: Locale) {
        val expected = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale))
        listOf("start date", "end date").forEach { name ->
            assertEquals("$name must follow app locale $locale", expected, field(name, locale).text)
        }
    }

    private fun repeatField() = UiWaitUtils.waitElement(
        UiSelectorParams(textMatches = "^(Never|Daily|Weekly|Every two weeks|Every four weeks)$")
    )

    private fun scheduleButton(): UiObject2 {
        var node = text("Schedule")
        while (node.className != "android.widget.Button" && !node.isClickable) {
            node = requireNotNull(node.parent) { "No Schedule button for its label" }
        }
        return node
    }

    private fun participantIsSelected(row: UiObject2): Boolean =
        row.isSelected || row.isChecked || row.findObject(By.checked(true)) != null

    fun isScheduleEnabled(): Boolean = scheduleButton().isEnabled

    private fun normalizedTime(value: String): String = value.replace('\u202f', ' ').replace('\u00a0', ' ')
}
