/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package uiautomatorutils

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import uiautomatorutils.UiWaitUtils.toBySelector
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

/** Restores wall clock using elapsed real time, even if the test moved the clock across midnight. */
@Suppress("TooManyFunctions")
class DeviceSettingsHelper(private val device: UiDevice) : AutoCloseable {
    private val originalTime = System.currentTimeMillis()
    private val originalElapsed = SystemClock.elapsedRealtime()
    private val originalZone = shell("getprop persist.sys.timezone")
    private val autoTime = shell("settings get global auto_time")
    private val autoZone = shell("settings get global auto_time_zone")
    private val timeFormat = shell("settings get system time_12_24")
    private var originalLocales: List<String>? = null
    private val originalLocaleSetting = shell("settings get system system_locales")
    private val supportsLocaleCommand by lazy { shell("cmd locale help").contains("set-device-locale") }
    private val localeDragHandle = UiSelectorParams(resourceId = "com.android.settings:id/dragHandle")
    private val addLanguage = UiSelectorParams(resourceId = "com.android.settings:id/add_language")
    private val languageSearch = UiSelectorParams(resourceId = "android:id/locale_search_menu")
    private val languageSearchInput = UiSelectorParams(resourceId = "android:id/search_src_text")
    private val languageCheckbox = UiSelectorParams(resourceId = "com.android.settings:id/checkbox")
    private val languageOverflow = UiSelectorParams(description = "More options")
    private val removeLanguage = UiSelectorParams(description = "Remove")
    private val settingsConfirmation = UiSelectorParams(resourceId = "com.android.settings:id/button_ok")
    private val localeMenu = UiSelectorParams(resourceId = "com.android.settings:id/settingslib_menu_button")
    private val menuTitle = UiSelectorParams(resourceId = "android:id/title")
    private val confirmLocale = UiSelectorParams(resourceId = "android:id/button1")
    private var clockChanged = false
    private var zoneChanged = false
    private var formatChanged = false

    fun setZone(zone: ZoneId) {
        require(zone.id.matches(Regex("[A-Za-z0-9_+./-]+")))
        zoneChanged = true
        shell("settings put global auto_time_zone 0")
        shell("cmd alarm set-timezone ${zone.id}")
        check(UiWaitUtils.retryUntilTimeout(5.seconds) { shell("getprop persist.sys.timezone") == zone.id }) {
            "Device did not accept timezone ${zone.id}"
        }
    }

    fun setTime(time: Instant) {
        clockChanged = true
        shell("settings put global auto_time 0")
        shell("cmd alarm set-time ${time.toEpochMilli()}")
        check(abs(System.currentTimeMillis() - time.toEpochMilli()) < CLOCK_TOLERANCE_MILLIS) {
            "Device did not accept the requested wall clock"
        }
    }

    fun useLocaleTimeFormat() {
        formatChanged = true
        shell("settings delete system time_12_24")
    }

    fun setLocale(locale: Locale) {
        val locales = deviceLocales()
        val tag = locale.toLanguageTag()
        if (originalLocales == null) originalLocales = locales
        if (locales.size == 1 && supportsLocaleCommand) {
            shell("cmd locale set-device-locale $tag")
            waitForLocales(listOf(tag))
        } else {
            if (tag !in locales) {
                withLanguageSettings {
                    UiWaitUtils.waitElement(addLanguage).click()
                    check(
                        UiWaitUtils.retryUntilTimeout(10.seconds) {
                            if (device.hasObject(languageSearchInput.toBySelector())) {
                                true
                            } else {
                                device.findObject(languageSearch.toBySelector())?.click()
                                false
                            }
                        }
                    ) { "Language search did not open" }
                    UiWaitUtils.waitElement(languageSearchInput).text = locale.getDisplayLanguage(locale)
                    UiWaitUtils.waitElement(languageChoice(locale.getDisplayLanguage(locale))).click()
                    UiWaitUtils.waitElement(languageChoice(locale.getDisplayCountry(locale))).click()
                    waitForLocales(locales + tag)
                }
            }
            moveLocaleToTop(tag)
        }
    }

    private fun languageChoice(text: String) = UiSelectorParams(resourceId = "android:id/locale", text = text)

    private fun deviceLocales(): List<String> {
        val stored = shell("settings get system system_locales")
        // Factory images may not persist this setting until the first language change.
        val tags = if (stored.isBlank() || stored == "null") {
            InstrumentationRegistry.getInstrumentation().context.resources.configuration.locales.toLanguageTags()
        } else {
            stored
        }
        return tags.split(',').also { locales ->
            check(locales.all { it.matches(Regex("[A-Za-z0-9-]+")) }) { "Cannot read device locales: $locales" }
        }
    }

    private fun waitForLocales(expected: List<String>) {
        check(UiWaitUtils.retryUntilTimeout(10.seconds) { deviceLocales() == expected }) {
            "Device locales did not change to $expected; actual: ${deviceLocales()}"
        }
    }

    private fun moveLocaleToTop(tag: String) {
        val locales = deviceLocales()
        val index = locales.indexOf(tag)
        require(index >= 0) { "Device language $tag is no longer configured" }
        if (index == 0) return
        withLanguageSettings {
            UiWaitUtils.waitElement(addLanguage, timeout = 10.seconds)
            val handles = device.findObjects(localeDragHandle.toBySelector()).sortedBy { it.visibleBounds.top }
            if (handles.isNotEmpty()) {
                check(handles.size == locales.size) { "All preferred languages must be visible to reorder them" }
                val destination = handles.first().visibleBounds
                val source = handles[index].visibleCenter
                device.swipe(source.x, source.y, destination.centerX(), destination.top, LANGUAGE_DRAG_STEPS)
                UiWaitUtils.waitElement(settingsConfirmation).click()
            } else {
                val menus = device.findObjects(localeMenu.toBySelector()).sortedBy { it.visibleBounds.top }
                check(menus.size == locales.size) { "The device must show a reorder menu for each preferred language" }
                menus[index].click()
                UiWaitUtils.waitUntilGoneOrThrow(localeMenu.toBySelector(), 5.seconds, "Language reorder menu did not open")
                UiWaitUtils.waitElement(menuTitle).click()
                UiWaitUtils.waitElement(confirmLocale).click()
            }
            waitForLocales(listOf(tag) + locales.filterNot { it == tag })
        }
    }

    private fun withLanguageSettings(action: () -> Unit) {
        val previousPackage = device.currentPackageName
        try {
            shell("am start -W -a android.settings.LOCALE_SETTINGS -f 0x10008000")
            device.waitForIdle()
            action()
        } finally {
            previousPackage?.let { packageName ->
                val intent = InstrumentationRegistry.getInstrumentation().context.packageManager.getLaunchIntentForPackage(packageName)
                intent?.component?.let { shell("am start -W -n ${it.flattenToString()}") }
            }
        }
    }

    private fun restoreLocales() {
        originalLocales?.let { locales ->
            if (locales.size == 1 && supportsLocaleCommand) {
                shell("cmd locale set-device-locale ${locales.single()}")
            } else if (deviceLocales() != locales) {
                locales.asReversed().forEach { moveLocaleToTop(it) }
                removeAddedLanguages(locales)
            }
            waitForLocales(locales)
            restoreSetting("system", "system_locales", originalLocaleSetting)
        }
    }

    private fun removeAddedLanguages(original: List<String>) {
        val current = deviceLocales()
        if (current.all { it in original }) return
        withLanguageSettings {
            UiWaitUtils.waitElement(languageOverflow).click()
            UiWaitUtils.waitElement(menuTitle).click()
            UiWaitUtils.waitElement(languageCheckbox)
            val boxes = device.findObjects(languageCheckbox.toBySelector()).sortedBy { it.visibleBounds.top }
            check(boxes.size == current.size) { "All languages must be visible to restore the original list" }
            current.forEachIndexed { index, tag -> if (tag !in original) boxes[index].click() }
            UiWaitUtils.waitElement(removeLanguage).click()
            UiWaitUtils.waitElement(settingsConfirmation).click()
            waitForLocales(original)
        }
    }

    @Suppress("TooGenericExceptionCaught") // Every setting must be restored even if an earlier restoration fails.
    override fun close() {
        // Attempt every restoration even when one command fails.
        val restorations = listOf<() -> Unit>(
            { restoreLocales() },
            { if (formatChanged) restoreSetting("system", "time_12_24", timeFormat) },
            { if (clockChanged) shell("cmd alarm set-time ${originalTime + SystemClock.elapsedRealtime() - originalElapsed}") },
            { if (zoneChanged) shell("cmd alarm set-timezone $originalZone") },
            { if (clockChanged) restoreSetting("global", "auto_time", autoTime) },
            { if (zoneChanged) restoreSetting("global", "auto_time_zone", autoZone) },
        )
        var failure: Throwable? = null
        restorations.forEach { restore ->
            try {
                restore()
            } catch (error: Exception) {
                if (failure == null) failure = error else failure?.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }

    private fun restoreSetting(table: String, key: String, value: String) {
        if (value == "null") shell("settings delete $table $key") else shell("settings put $table $key $value")
    }

    private companion object {
        const val LANGUAGE_DRAG_STEPS = 100
        const val CLOCK_TOLERANCE_MILLIS = 3000
    }

    private fun shell(command: String): String = device.executeShellCommand(command).trim().also {
        check(
            !it.contains("SecurityException") && !it.contains("Error:") &&
                !it.contains("Exception occurred") && !it.contains("Unknown command")
        ) {
            "Device settings command failed: $command: $it"
        }
    }
}
