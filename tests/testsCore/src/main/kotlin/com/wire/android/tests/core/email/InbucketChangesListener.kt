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
package com.wire.android.tests.core.email

import com.wire.android.tests.core.models.Message
import com.wire.android.tests.core.utils.Timedelta
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class InbucketChangesListener(
    private val mailbox: InbucketMailbox,
    expectedHeaders: Map<String, String>,
    timeout: Timedelta,
    rejectMessagesBefore: Timedelta
) : AbstractMailboxChangesListener(mailbox, expectedHeaders, timeout, rejectMessagesBefore) {

    // Example date: "2022-07-13T14:59:38.314900766Z"
    private val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")

    @Throws(Exception::class)
    override fun call(): String {
        val deliveredTo = mailbox.emailAddress
        val started = Timedelta.now()

        do {
            val messages = mailbox.getRecentMessages(deliveredTo)
            log.info("Got ${messages.size} incoming message(s) for $deliveredTo")

            for (message in messages) {
                if (areAllHeadersInMessage(message)) {
                    val messageDate = Instant.parse(message.date)
                    val listeningStartDate = Instant.ofEpochSecond(rejectMessagesBefore.asSeconds().toLong())

                    if (messageDate.isAfter(listeningStartDate)) {
                        log.info("Message accepted because message date ($messageDate) is after start of listening ($listeningStartDate)")
                        return mailbox.getMessageSource(deliveredTo, message.id)
                    }
                    log.severe("Message rejected because message date ($messageDate) is before start of listening ($listeningStartDate)")
                }
            }
            Timedelta.ofSeconds(2.0).sleep()
        } while (Timedelta.now().isDiffLessOrEqual(started, timeout))

        throw RuntimeException("Email message containing headers ${expectedHeaders} has not been found within $timeout")
    }

    protected fun areAllHeadersInMessage(message: Message): Boolean {
        for ((expectedHeaderName, expectedHeaderValue) in expectedHeaders) {
            var isHeaderFound = false
            log.info("Checking if the email message contains $expectedHeaderName: $expectedHeaderValue header")

            val headerValues = message.header?.get(expectedHeaderName)
            if (headerValues != null) {
                for (headerValue in headerValues) {
                    log.info("$expectedHeaderName: $headerValue -> $expectedHeaderValue")
                    if (headerValue == expectedHeaderValue) {
                        log.info("The expected header value '$expectedHeaderValue' is found in the email")
                        isHeaderFound = true
                        break
                    }
                }
            }
            log.info("Header $expectedHeaderName with value $expectedHeaderValue found: $isHeaderFound")
            if (!isHeaderFound) return false
        }
        return true
    }
}
