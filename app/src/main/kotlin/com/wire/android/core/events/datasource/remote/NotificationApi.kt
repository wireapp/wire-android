package com.wire.android.core.events.datasource.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NotificationApi {

    @GET("$NOTIFICATIONS$LAST")
    suspend fun lastNotification(@Query(CLIENT_QUERY_KEY) client: String): Response<LastNotificationResponse>

    @GET(NOTIFICATIONS)
    suspend fun notificationsByBatch(
        @Query(SIZE_QUERY_KEY) size: Int,
        @Query(CLIENT_QUERY_KEY) client: String,
        @Query(SINCE_QUERY_KEY) since: String
    ): Response<NotificationResponse>

    companion object {
        private const val NOTIFICATIONS = "/notifications"
        private const val LAST = "/last"
        private const val SIZE_QUERY_KEY = "size"
        private const val CLIENT_QUERY_KEY = "client"
        private const val SINCE_QUERY_KEY = "since"
    }
}
