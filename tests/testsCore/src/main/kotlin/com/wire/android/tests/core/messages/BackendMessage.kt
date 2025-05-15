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
package com.wire.android.tests.core.messages

import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Enumeration
import javax.mail.*
import javax.mail.internet.MimeMessage


open class BackendMessage(rawMsg: String) {
    val content: String
    private val mapHeaders = mutableMapOf<String, String>()

    init {
        val msg = stringToMsg(rawMsg)
        val hdrs: Enumeration<Header> = msg.allHeaders as Enumeration<Header>
        while (hdrs.hasMoreElements()) {
            val hdr = hdrs.nextElement()
            mapHeaders[hdr.name] = hdr.value
        }
        content = when (val msgContent = msg.content) {
            is Multipart -> processMultipart(msgContent)
            else -> msgContent.toString()
        }
    }

    fun getHeaderValue(headerName: String): String? = mapHeaders[headerName]

    private fun processMultipart(multipart: Multipart): String {
        var multipartContent = ""
        for (j in 0 until multipart.count) {
            val bodyPart = multipart.getBodyPart(j)
            if (bodyPart.disposition == null) {
                getText(bodyPart)?.let { multipartContent += it }
            }
        }
        return multipartContent
    }

    @Throws(MessagingException::class, IOException::class)
    private fun getText(p: Part): String? {
        if (p.isMimeType("text/*")) {
            return p.content as String
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            val mp = p.content as Multipart
            var text: String? = null
            for (i in 0 until mp.count) {
                val bp = mp.getBodyPart(i)
                when {
                    bp.isMimeType("text/plain") -> {
                        if (text == null) text = getText(bp)
                    }
                    bp.isMimeType("text/html") -> {
                        getText(bp)?.let { return it }
                    }
                    else -> return getText(bp)
                }
            }
            return text
        } else if (p.isMimeType("multipart/*")) {
            val mp = p.content as Multipart
            for (i in 0 until mp.count) {
                getText(mp.getBodyPart(i))?.let { return it }
            }
        }

        return null
    }

    //companion object {
        @Throws(MessagingException::class)
        fun stringToMsg(rawMsg: String): Message {
            val session = Session.getInstance(System.getProperties(), null)
            return MimeMessage(session, ByteArrayInputStream(rawMsg.toByteArray()))
        }
//    }
}
