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
@file:Suppress("ArgumentListWrapping")

package com.wire.android.tests.core.e2eTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.BackendClient
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class FileSharingRestrictionsTest : BaseUiTest() {

    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching { cleanupBackendClient(backendClient, currentUser) }
    }

    @TestCaseId("TC-8120")
    @Category("fileSharingRestrictions", "regression", "RC")
    @Ignore("Blocked: Kotlin test support does not expose a helper to unlock/disable team file sharing feature yet.")
    @Test
    fun givenTeamFileSharingDisabled_whenUserReceivesFeatureUpdate_thenTeamSettingsChangedAlertIsShown() {
        // TC mapping only. Needs backend helper parity for disabling team file sharing and alert selectors.
    }

    @TestCaseId("TC-8121", "TC-8147", "TC-8148")
    @Category("fileSharingRestrictions", "regression", "RC")
    @Ignore("Blocked: needs backend helper to disable file sharing plus stable remote device media sends for image/video/audio.")
    @Test
    fun givenTeamFileSharingDisabled_whenReceivingMedia_thenProhibitedPlaceholdersAreShown() {
        // TC mapping only. ConversationViewPage now has selectors for the prohibited placeholder texts.
    }

    @TestCaseId("TC-8149", "TC-8047")
    @Category("fileSharingRestrictions", "regression", "RC")
    @Ignore(
        "Blocked: needs backend helper to disable file sharing plus remote file send; attachment options can be asserted once setup exists."
    )
    @Test
    fun givenTeamFileSharingDisabled_whenReceivingFile_thenFilePlaceholderAndNoSharingOptionsAreShown() {
        // TC mapping only. ConversationViewPage exposes sharing-option negative assertions for later activation.
    }

    @TestCaseId("TC-8048", "TC-8051", "TC-8050")
    @Category("fileSharingRestrictions", "column", "blocked")
    @Ignore("Blocked: column backend file-sharing restriction setup and stable remote image/video/audio sends are not available yet.")
    @Test
    fun givenColumnTeamFileSharingDisabled_whenReceivingMedia_thenAllowedMediaRemainVisible() {
        // TC mapping only. Needs column login/2FA, disable-file-sharing helper, and remote media sends.
    }

    @TestCaseId("TC-8049")
    @Category("fileSharingRestrictions", "column", "blocked")
    @Ignore("Blocked: needs column disable-file-sharing helper, generated zip remote send, and no-download-alert assertion.")
    @Test
    fun givenColumnTeamFileSharingDisabled_whenReceivingGenericFile_thenFilePlaceholderCannotBeDownloaded() {
        // TC mapping only. Requires generic file send with MIME/size and file action modal negative assertion.
    }

    @TestCaseId("TC-8053", "TC-8054")
    @Category("fileSharingRestrictions", "column", "blocked")
    @Ignore("Blocked: needs column disable-file-sharing setup plus stable gallery image picker and audio recorder helpers.")
    @Test
    fun givenColumnTeamFileSharingDisabled_whenSendingAllowedMedia_thenImageAndRecordedAudioCanBeSent() {
        // TC mapping only. Image path overlaps FileSharingTest; audio path needs recorder controls and preview/send helpers.
    }

    @TestCaseId("TC-8052")
    @Category("fileSharingRestrictions", "column", "blocked")
    @Ignore("Blocked: needs column disable-file-sharing setup, arbitrary DocumentsUI file selection, and forbidden-send toast parity.")
    @Test
    fun givenColumnTeamFileSharingDisabled_whenSendingGenericFile_thenFileSendIsForbidden() {
        // TC mapping only. Requires push/select textfile.zip and assert it is absent after forbidden toast.
    }
}
