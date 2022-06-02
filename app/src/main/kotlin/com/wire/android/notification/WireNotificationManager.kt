package com.wire.android.notification

import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WireNotificationManager @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val messagesManager: MessageNotificationManager,
) {
    /**
     * Sync all the Pending events, fetch Message notifications from DB once and show it.
     * Can be used in Services (e.g., after receiving FCM)
     * @param userIdValue String value param of QualifiedID of the User that need to check Notifications for
     */
    suspend fun fetchAndShowNotificationsOnce(userIdValue: String) {
        checkIfUserIsAuthenticated(userId = userIdValue)?.let { userId ->
            coreLogic.getSessionScope(userId).syncPendingEvents()
            fetchAndShowMessageNotificationsOnce(userId)
            fetchAndShowCallNotificationsOnce(userId)
        }
    }

    private suspend fun fetchAndShowCallNotificationsOnce(userId: QualifiedID) {
        //TODO
    }

    private suspend fun fetchAndShowMessageNotificationsOnce(userId: QualifiedID) {
        val notificationsList = coreLogic.getSessionScope(userId)
            .messages
            .getNotifications()
            .first()

        messagesManager.handleNotification(notificationsList, userId)
    }

    // TODO: to be changed as soon as we get the qualifiedID from the notification payload
    /**
     * return the userId if the user is authenticated and null otherwise
     */
    @Suppress("NestedBlockDepth")
    private fun checkIfUserIsAuthenticated(userId: String): QualifiedID? =
        coreLogic.getAuthenticationScope().getSessions().let {
            when (it) {
                is GetAllSessionsResult.Success -> {
                    for (sessions in it.sessions) {
                        if (sessions.userId.value == userId)
                            return@let sessions.userId
                    }
                    null
                }
                is GetAllSessionsResult.Failure.Generic -> {
                    appLogger.e("get sessions failed ${it.genericFailure} ")
                    null
                }
                GetAllSessionsResult.Failure.NoSessionFound -> null
            }
        }


    /**
     * Infinitely listen for the new Message notifications and show it.
     * Can be used for listening for the Notifications when the app is running.
     * @param userIdFlow Flow of QualifiedID of User
     */
    @ExperimentalCoroutinesApi
    suspend fun observeMessageNotifications(userIdFlow: Flow<UserId?>) {
        userIdFlow
            .flatMapLatest { userId ->
                if (userId != null) {
                    coreLogic.getSessionScope(userId)
                        .messages
                        .getNotifications()
                } else {
                    // if userId == null means there is no current user (e.g., logged out)
                    // so we need to unsubscribe from the notification changes (it's done by `flatMapLatest`)
                    // and remove the notifications that were displayed previously
                    // (empty list in here makes all the pre. notifications be removed)
                    flowOf(listOf())
                }
                    // combining all the data that is necessary for Notifications into small data class,
                    // just to make it more readable than
                    // Pair<List<LocalNotificationConversation>, QualifiedID?>
                    .map { newNotifications ->
                        MessagesNotificationsData(newNotifications, userId)
                    }
            }
            .collect { (newNotifications, userId) ->
                messagesManager.handleNotification(newNotifications, userId)
            }
    }

    private data class MessagesNotificationsData(
        val newNotifications: List<LocalNotificationConversation>,
        val userId: QualifiedID?
    )
}
