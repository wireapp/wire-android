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
package com.wire.android.util

import android.net.Uri
import androidx.core.net.toUri
import com.wire.android.util.deeplink.UserLinkQRMapper
import com.wire.kalium.logic.data.id.QualifiedID
import org.junit.Assert.assertEquals
import org.junit.Test

class UserLinkQRMapperTest {

    @Test
    fun givenAUriFullQualifiedUrl_thenMapCorrectly() {
        val uri: Uri = "wire://user/domain/user-id".toUri()
        val result = UserLinkQRMapper.fromDeepLinkToQualifiedId(uri, "defaultDomain")

        assertEquals(UserLinkQRMapper.UserLinkQRResult.Success(QualifiedID("user-id", "domain")), result)
    }

    @Test
    fun givenAUriWrongFormat_thenMapToError() {
        val uri: Uri = "wire://user".toUri()
        val result = UserLinkQRMapper.fromDeepLinkToQualifiedId(uri, "defaultDomain")

        assertEquals(UserLinkQRMapper.UserLinkQRResult.Failure, result)
    }

    @Test
    fun givenAUriFullUnqualified_thenMapCorrectlyWithDefaultDomain() {
        val uri: Uri = "wire://user/user-id".toUri()
        val result = UserLinkQRMapper.fromDeepLinkToQualifiedId(uri, "defaultDomain")

        assertEquals(UserLinkQRMapper.UserLinkQRResult.Success(QualifiedID("user-id", "defaultDomain")), result)
    }

    @Test
    fun givenAUriQualifiedButNotSupportedFormat_thenMapCorrectlyWithDefaultDomain() {
        val uri: Uri = "wire://user/USER-ID@domain".toUri()
        val result = UserLinkQRMapper.fromDeepLinkToQualifiedId(uri, "defaultDomain")

        assertEquals(UserLinkQRMapper.UserLinkQRResult.Success(QualifiedID("user-id", "domain")), result)
    }

    @Test
    fun givenAUriWithUpperCaseId_thenMapCorrectlyWithLowercaseId() {
        val uri: Uri = "wire://user/uSer-Id@domain".toUri()
        val result = UserLinkQRMapper.fromDeepLinkToQualifiedId(uri, "defaultDomain")

        assertEquals(UserLinkQRMapper.UserLinkQRResult.Success(QualifiedID("user-id", "domain")), result)
    }
}
