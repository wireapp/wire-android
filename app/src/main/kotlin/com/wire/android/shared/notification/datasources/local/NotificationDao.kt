package com.wire.android.shared.notification.datasources.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notification WHERE conversation_id = :conversationId")
    suspend fun notificationsByConversationId(conversationId: String) : List<NotificationEntity>

    @Query("DELETE FROM notification WHERE conversation_id = :conversationId")
    suspend fun deleteNotificationsByConversationId(conversationId: String)

}
