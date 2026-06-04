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
package com.wire.android.tests.support

import android.Manifest
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import uiautomatorutils.PermissionUtils.grantRuntimePermsForApp

const val TIMEOUT_IN_MILLISECONDS = 20_000L

object UiAutomatorSetup {

    const val APP_DEV: String = "com.waz.zclient.dev.debug"
    const val APP_STAGING: String = "com.waz.zclient.dev"
    const val APP_BETA: String = "com.wire.android.internal"
    const val APP_PROD: String = "com.wire"
    const val APP_ALPHA: String = "com.wire.internal"
    lateinit var appPackage: String

    fun start(appPackage: String, clearData: Boolean = true): UiDevice {
        this.appPackage = appPackage

        val device = getDevice()

        if (clearData) {
            device.executeShellCommand("pm clear $appPackage")
        }

        grantNotificationPermissionIfSupported(appPackage)

        device.executeShellCommand("settings put secure show_ime_with_hard_keyboard 0")
        device.executeShellCommand("settings put global window_animation_scale 0")
        device.executeShellCommand("settings put global transition_animation_scale 0")
        device.executeShellCommand("settings put global animator_duration_scale .5")
        device.pressHome()

        waitForLauncher(device)
        startApp()
        waitAppStart(device)

        return device
    }

    fun getDevice(): UiDevice {
        return UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private fun waitAppStart(device: UiDevice) {
        device.wait(Until.hasObject(By.pkg(appPackage).depth(0)), TIMEOUT_IN_MILLISECONDS)
    }

    private fun startApp() {
        val context: Context = getApplicationContext()
        val intent = context.packageManager.getLaunchIntentForPackage(appPackage)
        try {
            intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        } catch (e: Exception) {
            throw RuntimeException("You must have the Wire app installed to run this test.")
        }
        context.startActivity(intent)
    }

    private fun waitForLauncher(device: UiDevice) {
        val launcherPackage = getLauncherPackageName()
        assertThat(launcherPackage, CoreMatchers.notNullValue())
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), TIMEOUT_IN_MILLISECONDS)
    }

    private fun getLauncherPackageName(): String {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        val context: Context = getApplicationContext()
        val packageManager = context.packageManager
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo!!.activityInfo.packageName
    }

    fun stopApp() {
        val device = getDevice()
        device.executeShellCommand("am force-stop $appPackage")
    }

    fun upgradeWireToRecentVersion(apkPath: String) {
        val device = getDevice()
        // Log and verify the installed version so CI proves that the APK upgrade really happened.
        val versionBeforeUpgrade = getInstalledWireVersion()
        reportUpgradeLog("Installed Wire before upgrade: $versionBeforeUpgrade")
        reportUpgradeLog("Upgrading Wire using APK: $apkPath")

        val output = device.executeShellCommand("pm install -r -d -g $apkPath").trim()
        val versionAfterUpgrade = getInstalledWireVersion()
        reportUpgradeLog("Installed Wire after upgrade: $versionAfterUpgrade")
        if (versionAfterUpgrade.versionCode <= versionBeforeUpgrade.versionCode) {
            val installOutput = output.ifBlank { "<empty>" }
            throw IllegalStateException(
                "Wire was not upgraded using APK from '$apkPath'. " +
                        "Before: $versionBeforeUpgrade. After: $versionAfterUpgrade. Install output: $installOutput"
            )
        }

        startApp()
        waitAppStart(device)
    }

    private fun reportUpgradeLog(message: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(
            0,
            Bundle().apply {
                putString(Instrumentation.REPORT_KEY_STREAMRESULT, message)
            }
        )
    }

    // Reads the installed Wire version so upgrade tests can compare the app before and after installation.
    private fun getInstalledWireVersion(): InstalledWireVersion {
        val context: Context = getApplicationContext()
        val packageInfo = context.packageManager.getPackageInfo(appPackage, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        val versionName = packageInfo.versionName ?: "<unknown>"

        return InstalledWireVersion(versionName, versionCode)
    }

    // Setup-level wrapper that pre-grants notifications on Android 13+ via PermissionUtils.
    private fun grantNotificationPermissionIfSupported(appPackage: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        runCatching {
            grantRuntimePermsForApp(appPackage, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private data class InstalledWireVersion(
        val versionName: String,
        val versionCode: Long
    ) {
        override fun toString(): String = "versionName=$versionName versionCode=$versionCode"
    }
}
