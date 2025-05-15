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
package com.wire.android.tests.core.utils

import com.wire.android.tests.core.config.Config
import java.util.regex.Pattern
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.emptyOrNullString
import org.hamcrest.Matchers.not

object MessagingUtils {

    fun getDefaultAccountName(): String {
        return Config.current().getDefaultEmail(MessagingUtils::class.java)
    }

    fun getSpecialAccountName(): String {
        return Config.current().getSpecialEmail(MessagingUtils::class.java)
    }

    fun getSpecialAccountPassword(): String {
        return Config.current().getSpecialEmailPassword(MessagingUtils::class.java)
    }

    fun generateEmail(basemail: String?, suffix: String): String {
        // FIXME: This is only a hack. Maybe remove MessagingUtils class completely and find a better solution
        if (basemail.isNullOrEmpty() || basemail == "true") {
            return "$suffix@wire.engineering"
        }
        val parts = basemail.split("@")
        return "${parts[0]}+$suffix@${parts[1]}"
    }

    fun get2FAVerificationCode(emailContent: String): String {
        var code: String? = null
        val p = Pattern.compile("([0-9]{6})", Pattern.CASE_INSENSITIVE)
        val urlMatcher = p.matcher(emailContent)
        while (urlMatcher.find()) {
            code = urlMatcher.group(0)
        }
        assertThat("Could not find code in mail", code, not(emptyOrNullString()))
        return code!!
    }

    fun getRecipientEmailFromHeader(emailContent: String): String {
        val p = Pattern.compile("To: <?(.*@wire.engineering)>?", Pattern.CASE_INSENSITIVE)
        val urlMatcher = p.matcher(emailContent)
        while (urlMatcher.find()) {
            return urlMatcher.group(1)
        }
        throw RuntimeException("Could not find recipient email address in mail: $emailContent")
    }
}
