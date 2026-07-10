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
package uiautomatorutils

import android.Manifest
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

object PermissionUtils {

    // Grants runtime permissions directly to a known app package.
    fun grantRuntimePermsForApp(pkg: String, vararg permissions: String) {
        val ui = InstrumentationRegistry.getInstrumentation().uiAutomation

        permissions.forEach { perm ->
            ui.executeShellCommand("pm grant $pkg $perm").close()
        }
    }

    // Grants runtime permissions to whichever app is currently in the foreground.
    fun grantRuntimePermsForForegroundApp(device: UiDevice, vararg permissions: String) {
        val pkg = device.currentPackageName
        grantRuntimePermsForApp(pkg, *permissions)
    }

    // Accessing files on Android 12 (API 32) and below requires storage permissions.
    fun grantStoragePermissionsIfSupported(appPackage: String) {
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                grantRuntimePermsForApp(appPackage, Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                grantRuntimePermsForApp(appPackage, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
}
