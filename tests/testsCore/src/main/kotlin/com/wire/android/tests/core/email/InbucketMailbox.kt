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

import com.wire.android.tests.core.models.InbucketClient
import com.wire.android.tests.core.models.Message
import com.wire.android.tests.core.services.ISupportsMessagesPolling
import com.wire.android.tests.core.services.backend.Backend
import com.wire.android.tests.core.utils.Timedelta
import com.wire.android.tests.core.utils.ZetaLogger
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.logging.Logger

class InbucketMailbox(backend: Backend, val emailAddress: String) : ISupportsMessagesPolling {

    companion object {
        private val log: Logger = ZetaLogger.getLog(InbucketMailbox::class.simpleName)
        private const val MAX_PARALLEL_EMAIL_LISTENER_TASKS = 4
    }

    private val client = backend.getInbucketUsername()?.let {
        InbucketClient(
            backend.inbucketUrl,
            it,
            backend.getInbucketPassword()!!,
            backend.useProxy()
        )
    }

    private val pool: ExecutorService = Executors.newFixedThreadPool(MAX_PARALLEL_EMAIL_LISTENER_TASKS)

    override fun waitUntilMessagesCountReaches(deliveredTo: String, expectedMsgsCount: Int, timeout: Timedelta): Boolean {
        var recentMessages = getRecentMessages(deliveredTo)
        val start = System.currentTimeMillis()
        val end = start + timeout.asMillis()

        while (System.currentTimeMillis() < end && recentMessages.size < expectedMsgsCount) {
            Thread.sleep(1000)
            recentMessages = getRecentMessages(deliveredTo)
        }
        return getRecentMessages(deliveredTo).size >= expectedMsgsCount
    }

    @Throws(Exception::class)
    override fun getMessage(expectedHeaders: Map<String, String>, timeout: Timedelta): Future<String> {
        val rejectMessagesBefore = Timedelta.ofMillis(0.0)
        return getMessage(expectedHeaders, timeout, rejectMessagesBefore)
    }

    @Throws(Exception::class)
    override fun getMessage(
        expectedHeaders: Map<String, String>,
        timeout: Timedelta,
        rejectMessagesBefore: Timedelta
    ): Future<String> {
        val listener = InbucketChangesListener(this, expectedHeaders, timeout, rejectMessagesBefore)
        log.fine("Started email listener for message containing headers ${expectedHeaders}...")
        return pool.submit(listener)
    }

    override fun isAlive(): Boolean = true

    @Throws(IOException::class)
    fun getRecentMessages(deliveredTo: String): List<Message> {
        if (client == null) return emptyList()
        val messageInfos = client.getMailbox(deliveredTo)
        return messageInfos.map { mi ->
            try {
                mi.id?.let { client.getMessage(deliveredTo, it) }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }!!
        }
    }

    @Throws(IOException::class)
    fun getMessageSource(deliveredTo: String, messageId: String): String {
        return client?.getMessageSource(deliveredTo, messageId) ?: "No message source"
    }
}
