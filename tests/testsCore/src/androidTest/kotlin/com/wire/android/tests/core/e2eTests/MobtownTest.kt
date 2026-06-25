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
package com.wire.android.tests.core.e2eTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MobtownTest {

    @TestCaseId("TC-4671")
    @Category("mobtown")
    @Ignore(
        "Blocked: Mobtown text-message flow needs mobtown backend deep link/login fixture plus stable remote-device " +
            "message delivery and unread-conversation notification handling."
    )
    @Test
    fun givenMobtownUser_whenSendingAndReceivingTextMessages_thenMessagingFlowIsMappedOnly() = Unit

    @TestCaseId("TC-4667")
    @Category("mobtown")
    @Ignore(
        "Blocked: Mobtown image flow needs both ingress backend fixtures, remote image-send helpers, QR image " +
            "assertions, and current DocumentsUI image picker parity."
    )
    @Test
    fun givenMobtownUser_whenSendingAndReceivingImages_thenImageFlowIsMappedOnly() = Unit

    @TestCaseId("TC-4668")
    @Category("mobtown")
    @Ignore(
        "Blocked: Mobtown file flow needs both ingress backend fixtures, remote generic-file send helper, and file " +
            "visibility/download assertions on Mobtown."
    )
    @Test
    fun givenMobtownUser_whenReceivingFiles_thenFileFlowIsMappedOnly() = Unit

    @TestCaseId("TC-4669", "TC-4670")
    @Category("mobtown", "links")
    @Ignore(
        "Blocked: Mobtown link checks need mobtown backend login plus browser URL assertions for reset-password and " +
            "support links, including negative assertion against the other ingress domain."
    )
    @Test
    fun givenMobtownUser_whenOpeningPasswordAndSupportLinks_thenIngressUrlsAreMappedOnly() = Unit
}
