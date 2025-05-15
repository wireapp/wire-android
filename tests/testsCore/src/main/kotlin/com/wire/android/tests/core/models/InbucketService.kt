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
package com.wire.android.tests.core.models

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface InbucketService {

    @GET("api/v1/mailbox/{name}")
    fun getMailbox(@Path("name") mailboxName: String): Call<List<MessageInfo>>

    @GET("api/v1/mailbox/{name}/{id}")
    fun getMessage(
        @Path("name") mailboxName: String,
        @Path("id") messageId: String
    ): Call<Message>

    @GET("api/v1/mailbox/{name}/{id}/source")
    fun getMessageSource(
        @Path("name") mailboxName: String,
        @Path("id") messageId: String
    ): Call<ResponseBody>

    @DELETE("api/v1/mailbox/{name}")
    fun deleteMailbox(@Path("name") mailboxName: String): Call<String>

    @DELETE("api/v1/mailbox/{name}/{id}/source")
    fun deleteMessage(
        @Path("name") mailboxName: String,
        @Path("id") messageId: String
    ): Call<String>
}
