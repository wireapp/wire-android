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
package com.wire.android.tests.core.pages

import androidx.test.uiautomator.UiDevice
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import user.usermanager.ClientUserManager
import kotlin.test.DefaultAsserter.assertTrue

data class BackupPage(private val device: UiDevice) {
    private fun backupFileLocator(uniqueUserName: String) = UiSelectorParams(textContains = "Wire-$uniqueUserName")
    private val backupPageHeading = UiSelectorParams(text = "Back up & Restore Conversations")
    private val restoreBackupButton = UiSelectorParams(text = "Restore from Backup")
    private val createBackupButton = UiSelectorParams(text = "Create a Backup")
    private val backUpNowButton = UiSelectorParams(text = "Back Up Now")
    private val saveFileButton = UiSelectorParams(text = "Save File")
    private val saveButtonOSMenu = UiSelectorParams(text = "SAVE")
    private val chooseBackupButton = UiSelectorParams(text = "Choose Backup File")
    private val backupPasswordInputField = UiSelectorParams(className = "android.widget.EditText")
    private val continueButton = UiSelectorParams(text = "Continue")
    private val okButton = UiSelectorParams(text = "OK")

    fun iSeeBackupPageHeading(): BackupPage {
        try {
            UiWaitUtils.waitElement(backupPageHeading)
        } catch (e: AssertionError) {
            throw AssertionError("Backup Page is not displayed", e)
        }
        return this
    }

    fun clickRestoreBackupButton(): BackupPage {
        UiWaitUtils.waitElement(restoreBackupButton).click()
        return this
    }

    fun clickCreateBackupButton(): BackupPage {
        UiWaitUtils.waitElement(createBackupButton).click()
        return this
    }

    fun clickBackUpNowButton(): BackupPage {
        val clicked = UiWaitUtils.clickWhenClickable(
            params = backUpNowButton,
            timeout = UiWaitUtils.LONG_TIMEOUT,
            pollingInterval = UiWaitUtils.POLLING_FAST
        )
        assertTrue("'Back Up Now' button was not clickable", clicked)
        return this
    }

    fun typeBackupPassword(password: String): BackupPage {
        val input = UiWaitUtils.waitElement(backupPasswordInputField)
        input.click()
        input.text = password
        return this
    }

    fun iTapSaveFileButton(): BackupPage {
        UiWaitUtils.waitElement(saveFileButton).click()
        return this
    }

    fun iTapSaveInOSMenuButton(): BackupPage {
        UiWaitUtils.waitElement(saveButtonOSMenu).click()
        return this
    }

    fun iSeeBackupConfirmation(text: String): BackupPage {
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = UiSelectorParams(textContains = text),
            timeout = UiWaitUtils.SHORT_TIMEOUT,
            errorMessage = "Expected message '$text' was not displayed"
        )
        return this
    }

    fun clickChooseBackupFileButton(): BackupPage {
        UiWaitUtils.waitElement(chooseBackupButton).click()
        return this
    }

    fun selectBackupFileInDocumentsUI(clientUserManager: ClientUserManager, userAlias: String): BackupPage {
        val user = clientUserManager.findUserBy(
            userAlias,
            ClientUserManager.FindBy.NAME_ALIAS
        )
        val uniqueUserName = user.uniqueUsername.orEmpty()
        try {
            UiWaitUtils.waitElement(backupFileLocator(uniqueUserName)).click()
        } catch (e: AssertionError) {
            throw AssertionError(
                "Backup file with name 'Wire-$uniqueUserName' not found in DocumentsUI",
                e
            )
        }
        return this
    }

    fun waitUntilThisTextIsDisplayedOnBackupAlert(text: String): BackupPage {
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = UiSelectorParams(text = text),
            timeout = UiWaitUtils.SHORT_TIMEOUT,
            errorMessage = "Text '$text' was not displayed on the backup alert within timeout"
        )
        return this
    }

    fun clickOkButtonOnBackupAlert(): BackupPage {
        UiWaitUtils.waitElement(okButton).click()
        return this
    }

    fun tapContinueButtonOnBackupPage(): BackupPage {
        val clicked = UiWaitUtils.clickWhenClickable(
            params = continueButton,
            timeout = UiWaitUtils.LONG_TIMEOUT,
            pollingInterval = UiWaitUtils.POLLING_FAST
        )
        assertTrue("'Continue' button on backup page was not clickable", clicked)
        return this
    }
}
