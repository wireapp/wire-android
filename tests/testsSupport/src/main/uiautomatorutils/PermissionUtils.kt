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

import android.graphics.Bitmap
import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.encoder.QRCode
import java.io.File
import java.io.FileOutputStream
import android.graphics.Color


object TestPermissionUtils {

    fun grantRuntimePermsForForegroundApp(device: UiDevice, vararg permissions: String) {
        val inst = InstrumentationRegistry.getInstrumentation()
        val pkg = device.currentPackageName
        val ui = inst.uiAutomation

        permissions.forEach { perm ->
            ui.executeShellCommand("pm grant $pkg $perm").close()
        }
    }
}


