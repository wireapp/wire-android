package com.wire.android.core.events.datasource.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler

class NotificationRemoteDataSource(
    private val notificationApi: NotificationApi,
    override val networkHandler: NetworkHandler
) : ApiService() {

    suspend fun lastNotification(client: String): Either<Failure, NotificationResponse> = request {
        notificationApi.lastNotification(client)
    }

    suspend fun notificationsByBatch(
        size: Int,
        client: String,
        since: String
    ): Either<Failure, NotificationPageResponse> =
        request { notificationApi.notificationsByBatch(size, client, since) }

}
