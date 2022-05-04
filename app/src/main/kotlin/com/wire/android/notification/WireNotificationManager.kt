package com.wire.android.notification

import com.wire.android.di.GetNotificationsUseCaseProvider
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
import kotlinx.coroutines.flow.scan
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WireNotificationManager @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val getNotificationProvider: GetNotificationsUseCaseProvider.Factory,
    private val notificationManager: MessageNotificationManager,
) {

    /**
     * Sync all the Pending events, fetch Message notifications from DB once and show it.
     * Can be used in Services (e.g., after receiving FCM)
     * @param userId QualifiedID of User that need to check Notifications for
     */
    suspend fun fetchAndShowMessageNotificationsOnce(userIdValue: String) {
        val userId = getQualifiedIDFromUserId(userId = userIdValue)
        coreLogic.getSessionScope(userId).syncPendingEvents()

        val notificationsList = getNotificationProvider.create(userId)
            .getNotifications()
            .first()

        notificationManager.handleNotification(listOf(), notificationsList, userId)
    }

    // todo to be deleted as soon as we get the qualifiedID from the notification payload
    private fun getQualifiedIDFromUserId(userId: String): QualifiedID {
        coreLogic.getAuthenticationScope().getSessions().let {
            when (it) {
                is GetAllSessionsResult.Success -> {
                    for (sessions in it.sessions) {
                        if (sessions.userId.value == userId) {
                            return sessions.userId
                        }
                    }
                }
                is GetAllSessionsResult.Failure -> {
                    return QualifiedID(userId, "wire.com")
                }
            }
        }
        return QualifiedID(userId, "wire.com")
    }

    /**
     * Infinitely listen for the new Message notifications and show it.
     * Can be used for listening for the Notifications when the app is running.
     * @param userIdFlow Flow of QualifiedID of User
     */
    @ExperimentalCoroutinesApi
    suspend fun listenForMessageNotifications(userIdFlow: Flow<UserId?>) {
        userIdFlow
            .flatMapLatest { userId ->
                if (userId != null) {
                    getNotificationProvider.create(userId).getNotifications()
                } else {
                    // if userId == null means there is no current user (e.g., logged out)
                    // so we need to unsubscribe from the notification changes (it's done by `flatMapLatest`)
                    // and remove the notifications that were displayed previously
                    // (empty list in here makes all the pre. notifications be removed)
                    flowOf(listOf())
                }
                    // we need to remember prev. displayed Notifications,
                    // so we can remove notifications that were displayed previously but are not in the new list
                    .scan((listOf<LocalNotificationConversation>() to listOf<LocalNotificationConversation>()))
                    { old, newList -> old.second to newList }
                    // combining all the data that is necessary for Notifications into small data class,
                    // just to make it more readable than
                    // Triple<List<LocalNotificationConversation>, List<LocalNotificationConversation>, QualifiedID?>
                    .map { (oldNotifications, newNotifications) ->
                        MessagesNotificationsData(oldNotifications, newNotifications, userId)
                    }
            }
            .collect { (oldNotifications, newNotifications, userId) ->
                notificationManager.handleNotification(oldNotifications, newNotifications, userId)
            }
    }

    private data class MessagesNotificationsData(
        val oldNotifications: List<LocalNotificationConversation>,
        val newNotifications: List<LocalNotificationConversation>,
        val userId: QualifiedID?
    )
}
