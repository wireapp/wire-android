package com.wire.android.notification

import android.app.NotificationChannelGroup
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.wire.android.appLogger
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelsManager @Inject constructor(
    private val context: Context,
    private val notificationManagerCompat: NotificationManagerCompat
) {
    private val incomingCallSoundUri by lazy { Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/ringing_from_them") }

    fun createNotificationChannels(allUsers: List<SelfUser>) {
        appLogger.i("${TAG}: creating all the channels for ${allUsers.size} users")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // creating regular NotificationChannels, that are common for all users and shouldn't be grouped.
        createRegularChannel(NotificationConstants.OTHER_CHANNEL_ID, NotificationConstants.OTHER_CHANNEL_NAME)
        createRegularChannel(NotificationConstants.WEB_SOCKET_CHANNEL_ID, NotificationConstants.WEB_SOCKET_CHANNEL_NAME)
        createRegularChannel(NotificationConstants.MESSAGE_SYNC_CHANNEL_ID, NotificationConstants.MESSAGE_SYNC_CHANNEL_NAME)

        // creating user-specific NotificationChannels for each user, they will be grouped by User in App Settings.
        allUsers.forEach { user ->
            val groupId = createNotificationChannelGroup(user.id, user.handle ?: user.name ?: user.id.value)

            createIncomingCallsChannel(groupId, user.id)
            createOngoingNotificationChannel(groupId, user.id)
            createMessagesNotificationChannel(user.id, groupId)
        }
    }

    /**
     * Deletes NotificationChanelGroup (and all NotificationChannels that belongs to it) for a specific User.
     * Use it on logout.
     */
    fun deleteChannelGroup(userId: UserId) {
        appLogger.i("${TAG}: deleting notification channels for ${userId.toString().obfuscateId()} user")
        notificationManagerCompat.deleteNotificationChannelGroup(NotificationConstants.getChanelGroupIdForUser(userId))
    }

    /**
     * @return created groupId.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannelGroup(userId: UserId, userName: String): String {
        val chanelGroupId = NotificationConstants.getChanelGroupIdForUser(userId)
        val channelGroup = NotificationChannelGroup(
            chanelGroupId,
            getChanelGroupNameForUser(userName)
        )
        notificationManagerCompat.createNotificationChannelGroup(channelGroup)
        return chanelGroupId
    }

    private fun createIncomingCallsChannel(groupId: String, userId: UserId) {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setUsage(getAudioAttributeUsageByOsLevel())
            .build()

        val chanelId = NotificationConstants.getIncomingChannelId(userId)
        val notificationChannel = NotificationChannelCompat
            .Builder(chanelId, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(NotificationConstants.INCOMING_CALL_CHANNEL_NAME)
            .setSound(incomingCallSoundUri, audioAttributes)
            .setShowBadge(false)
            .setVibrationEnabled(true)
            .setGroup(groupId)
            .build()

        notificationManagerCompat.createNotificationChannel(notificationChannel)
    }

    private fun createOngoingNotificationChannel(groupId: String, userId: UserId) {
        val chanelId = NotificationConstants.getOngoingChannelId(userId)
        val notificationChannel = NotificationChannelCompat
            .Builder(chanelId, NotificationManagerCompat.IMPORTANCE_MAX)
            .setName(NotificationConstants.ONGOING_CALL_CHANNEL_NAME)
            .setVibrationEnabled(false)
            .setImportance(NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setSound(null, null)
            .setGroup(groupId)
            .build()

        notificationManagerCompat.createNotificationChannel(notificationChannel)
    }

    private fun createMessagesNotificationChannel(userId: UserId, channelGroupId: String) {
        val notificationChannel = NotificationChannelCompat
            .Builder(NotificationConstants.getMessagesChannelId(userId), NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(NotificationConstants.MESSAGE_CHANNEL_NAME)
            .setGroup(channelGroupId)
            .build()

        notificationManagerCompat.createNotificationChannel(notificationChannel)
    }

    private fun createRegularChannel(channelId: String, channelName: String) {
        val notificationChannel = NotificationChannelCompat
            .Builder(channelId, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(channelName)
            .build()

        notificationManagerCompat.createNotificationChannel(notificationChannel)
    }

    /**
     * Tricky bug: No documentation whatsoever, but these values affect how the system cancels or not the vibration of the notification
     * on different Android OS levels, probably channel creation related.
     */
    private fun getAudioAttributeUsageByOsLevel() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AudioAttributes.USAGE_NOTIFICATION_RINGTONE else AudioAttributes.USAGE_MEDIA

    companion object {
        private fun getChanelGroupNameForUser(userName: String): String = userName

        private const val TAG = "NotificationChannelsManager"
    }
}
