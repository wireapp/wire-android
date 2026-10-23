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

import android.os.Build
import android.os.SystemClock
import androidx.test.uiautomator.UiDevice
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
    private val originalAppLocales = mutableMapOf<String, String>()
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

    fun setAppLocale(packageName: String, locale: Locale) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { "App locale commands require Android 13 or newer" }
        require(packageName.matches(Regex("[A-Za-z0-9_.]+")))
        originalAppLocales.getOrPut(packageName) { appLocales(packageName) }
        applyAppLocales(packageName, locale.toLanguageTag())
    }

    private fun appLocales(packageName: String): String {
        val output = shell("cmd locale get-app-locales $packageName")
        return requireNotNull(Regex("are \\[([A-Za-z0-9,-]*)]").find(output)?.groupValues?.get(1)) {
            "Cannot read app locales for $packageName: $output"
        }
    }

    private fun applyAppLocales(packageName: String, tags: String) {
        require(tags.matches(Regex("[A-Za-z0-9,-]*")))
        val argument = if (tags.isEmpty()) "" else " --locales $tags"
        shell("cmd locale set-app-locales $packageName$argument")
        check(UiWaitUtils.retryUntilTimeout(10.seconds) { appLocales(packageName) == tags }) {
            "App locales for $packageName did not change to '$tags'"
        }
    }

    @Suppress("TooGenericExceptionCaught") // Every setting must be restored even if an earlier restoration fails.
    override fun close() {
        // Attempt every restoration even when one command fails.
        val restorations = listOf<() -> Unit>(
            { originalAppLocales.forEach { (packageName, tags) -> applyAppLocales(packageName, tags) } },
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
