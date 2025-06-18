package com.wire.android.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.wire.android.R
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.UserId
import com.wire.kalium.logic.data.notification.LocalNotification
import com.wire.kalium.logic.data.notification.LocalNotificationMessage
import com.wire.kalium.logic.data.notification.LocalNotificationMessageAuthor
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import kotlinx.datetime.Instant

@ExtendWith(MockKExtension::class)
class MessageNotificationManagerTest {

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockNotificationManagerCompat: NotificationManagerCompat

    @MockK
    private lateinit var mockNotificationManager: NotificationManager

    @MockK
    private lateinit var mockLockCodeTimeManager: LockCodeTimeManager

    private lateinit var messageNotificationManager: MessageNotificationManager

    private val testUserId = UserId(UUID.randomUUID().toString(), "domain")
    private val testQualifiedId = QualifiedID(testUserId.value, testUserId.domain)
    private val testUserName = "Test User"
    private val testConversationId = ConversationId(UUID.randomUUID().toString(), "domain")

    @BeforeEach
    fun setUp() {
        // Mock static NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification
        // It's often problematic to mock static methods from libraries if not designed for it.
        // For now, assume it works or test via verifying inputs to Style constructor.
        mockkStatic(NotificationCompat.MessagingStyle::class)

        every { mockContext.getString(R.string.notification_receiver_name) } returns "You"
        // Mock other string resources if necessary, though current masking logic avoids many.

        messageNotificationManager = MessageNotificationManager(
            mockContext,
            mockNotificationManagerCompat,
            mockNotificationManager,
            mockLockCodeTimeManager
        )
    }

    private fun createTestLocalNotificationConversation(
        messages: List<LocalNotificationMessage>
    ): LocalNotification.Conversation {
        return LocalNotification.Conversation(
            conversationId = testConversationId,
            name = "Original Conversation Name",
            messages = messages,
            isReplyAllowed = true
        )
    }

    private fun createTestNotificationMessage(
        messageId: String = UUID.randomUUID().toString(),
        text: String = "Original message text",
        authorName: String = "Original Sender"
    ): LocalNotificationMessage {
        return LocalNotificationMessage.Text(
            messageId = messageId,
            author = LocalNotificationMessageAuthor(authorName, null),
            time = Instant.DISTANT_PAST, // Using a fixed time for consistent testing
            text = text
        )
    }

    @Test
    fun `handleNotification for new conversation shows generic title and masked messages`() {
        // Arrange
        val notificationMessage = createTestNotificationMessage()
        val localNotification = createTestLocalNotificationConversation(listOf(notificationMessage))
        val notifications = listOf(localNotification)

        val capturedNotification = slot<Notification>()
        every { mockNotificationManager.activeNotifications } returns emptyArray() // No existing notifications
        every { mockNotificationManagerCompat.notify(any(), capture(capturedNotification)) } returns Unit

        // Stubbing for NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification if it's called with null
        val mockMessagingStyle = mockk<NotificationCompat.MessagingStyle>(relaxed = true)
        every { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(any()) } returns null
        every { mockMessagingStyle.addMessage(any<NotificationCompat.MessagingStyle.Message>()) } returns mockMessagingStyle

        // Mock the MessagingStyle constructor call if possible, or verify its inputs.
        // This is tricky because it's `new NotificationCompat.MessagingStyle(person)`
        // For now, we'll inspect the final notification.

        // Act
        messageNotificationManager.handleNotification(notifications, testQualifiedId, testUserName)

        // Assert
        verify { mockNotificationManagerCompat.notify(any(), any()) }

        val notification = capturedNotification.captured
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)

        // If extractMessagingStyleFromNotification itself is part of what we changed by mistake,
        // we might need to inspect notification.extras for "android.messages" etc.
        // For now, assuming our previous change to `intoStyledMessage` and `getConversationTitle`
        // correctly populates the style upon creation within `addMessages`.

        // Due to mocking `extractMessagingStyleFromNotification` to return null, then the new style is created.
        // The assertions below are on the `Notification`'s direct fields if style is null,
        // or on the properties of the *newly created* style in `addMessages`.

        // The actual style construction happens in `addMessages` which calls `intoStyledMessage`
        // and `getConversationTitle`.
        // Let's verify the content of the `Notification` extras which store MessagingStyle data.
        val extras = notification.extras
        val conversationTitle = extras.getString(Notification.EXTRA_TITLE)
        assertEquals("New message", conversationTitle, "Conversation title should be generic.")

        val messages: Array<Parcelable>? = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        assert(messages?.isNotEmpty() ?: false) { "Should have messages in notification" }

        messages?.forEach { msgParcel ->
            val msgBundle = msgParcel as Bundle // Messages are Bundles
            assertEquals("Someone", (msgBundle.getParcelable("sender_person") as Person?)?.name, "Sender name should be generic.")
            assertEquals("You have a new message", msgBundle.getCharSequence("text"), "Message content should be generic.")
        }
    }

    private fun mockExistingStatusBarNotification(
        conversationIdStr: String,
        userIdStr: String,
        originalTitle: CharSequence?,
        messages: List<NotificationCompat.MessagingStyle.Message>
    ): android.service.notification.StatusBarNotification {
        val mockStatusBarNotification = mockk<android.service.notification.StatusBarNotification>()
        val mockExistingNotification = mockk<Notification>()
        val mockExtras = Bundle()

        if (originalTitle != null) {
            mockExtras.putCharSequence(Notification.EXTRA_TITLE, originalTitle)
        }
        val messageBundles = messages.map { msg ->
            Bundle().apply {
                putCharSequence("text", msg.text)
                putLong("time", msg.timestamp)
                putParcelable("sender_person", msg.person) // Person might need deeper mocking if accessed
                // Add MESSAGE_ID_EXTRA if your masking logic preserves it
                msg.extras.getString("message_id")?.let { putString("message_id", it) }
            }
        }.toTypedArray()
        mockExtras.putParcelableArray(Notification.EXTRA_MESSAGES, messageBundles)
        // Simulate a MessagingStyle notification
        mockExtras.putString(Notification.EXTRA_TEMPLATE, "android.app.Notification\$MessagingStyle")


        every { mockExistingNotification.extras } returns mockExtras
        every { mockExistingNotification.contentIntent } returns mockk(relaxed = true) // For getUpdatedConversationNotification
        every { mockExistingNotification.actions } returns null // For getUpdatedConversationNotification
        every { mockExistingNotification.getLargeIcon() } returns null // For getUpdatedConversationNotification

        every { mockStatusBarNotification.notification } returns mockExistingNotification
        every { mockStatusBarNotification.id } returns NotificationConstants.getConversationNotificationId(conversationIdStr, userIdStr)
        every { mockStatusBarNotification.tag } returns "someTag" // Example tag
        every { mockStatusBarNotification.groupKey } returns NotificationConstants.getMessagesGroupKey(testQualifiedId)


        // This is crucial for Notification.updateMessages to work
        val existingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName("You").build())
        existingStyle.conversationTitle = originalTitle
        messages.forEach { existingStyle.addMessage(it) }
        every { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(mockExistingNotification) } returns existingStyle

        return mockStatusBarNotification
    }

    @Test
    fun `handleNotification for update message maintains generic title and masked content`() {
        // Arrange
        val originalMessageId = "original_msg_id"
        val originalSender = Person.Builder().setName("Original Sender").build()
        val originalMsg = NotificationCompat.MessagingStyle.Message(
            "Original text",
            System.currentTimeMillis() - 1000,
            originalSender
        ).apply { extras.putString("message_id", originalMessageId) }

        val existingStatusBarNotification = mockExistingStatusBarNotification(
            testConversationId.toString(),
            testQualifiedId.toString(),
            "Original Conversation Title",
            listOf(originalMsg)
        )
        every { mockNotificationManager.activeNotifications } returns arrayOf(existingStatusBarNotification)

        val updateAction = LocalNotification.UpdateMessage.Action.Edit("Updated text, but will be masked", "new_message_id")
        val updateNotification = LocalNotification.UpdateMessage(testConversationId, "Original Name", updateAction)
        val notifications = listOf(updateNotification)

        val capturedNotification = slot<Notification>()
        every { mockNotificationManagerCompat.notify(any(), capture(capturedNotification)) } returns Unit

        // Act
        messageNotificationManager.handleNotification(notifications, testQualifiedId, testUserName)

        // Assert
        verify { mockNotificationManagerCompat.notify(any(), any()) }

        val notification = capturedNotification.captured
        val extras = notification.extras
        val conversationTitle = extras.getString(Notification.EXTRA_TITLE)
        assertEquals("New message", conversationTitle, "Conversation title should be generic after update.")

        val messages: Array<Parcelable>? = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        assert(messages?.isNotEmpty() ?: false) { "Should have messages in notification after update" }

        messages?.forEach { msgParcel ->
            val msgBundle = msgParcel as Bundle
            assertEquals("Someone", (msgBundle.getParcelable("sender_person") as Person?)?.name, "Sender name should be generic after update.")
            assertEquals("You have a new message", msgBundle.getCharSequence("text"), "Message content should be generic after update.")
        }
    }

    @Test
    fun `updateNotificationAfterQuickReply maintains generic title and masked content`() {
        // Arrange
        val originalMessageId = "original_msg_id"
        val originalSender = Person.Builder().setName("Original Sender").build()
        val originalMsgStyle = NotificationCompat.MessagingStyle.Message(
            "Original text",
            System.currentTimeMillis() - 1000,
            originalSender
        ).apply { extras.putString("message_id", originalMessageId) }

        val existingStatusBarNotification = mockExistingStatusBarNotification(
            testConversationId.toString(),
            testQualifiedId.toString(),
            "Original Conversation Title",
            listOf(originalMsgStyle)
        )

        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager
        every { mockNotificationManager.activeNotifications } returns arrayOf(existingStatusBarNotification)

        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(mockContext) } returns mockNotificationManagerCompat

        val capturedNotification = slot<Notification>()
        every { mockNotificationManagerCompat.notify(any(), capture(capturedNotification)) } returns Unit

        val replyText = "This is a test reply."

        // Act
        MessageNotificationManager.updateNotificationAfterQuickReply(
            mockContext,
            testConversationId.toString(),
            testQualifiedId,
            replyText
        )

        // Assert
        verify { mockNotificationManagerCompat.notify(NotificationConstants.getConversationNotificationId(testConversationId.toString(), testQualifiedId.toString()), any()) }

        val notification = capturedNotification.captured
        val extras = notification.extras
        val conversationTitle = extras.getString(Notification.EXTRA_TITLE) // EXTRA_CONVERSATION_TITLE is for group conv name, not overall title
        assertEquals("New message", conversationTitle, "Conversation title should be generic after quick reply.")


        val messages: Array<Parcelable>? = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        assert(messages?.size ?: 0 >= 2) { "Should have original messages and the reply" }

        messages?.forEach { msgParcel ->
            val msgBundle = msgParcel as Bundle
            assertEquals("Someone", (msgBundle.getParcelable("sender_person") as Person?)?.name, "Sender name should be generic after quick reply.")
            assertEquals("You have a new message", msgBundle.getCharSequence("text"), "Message content should be generic after quick reply.")
        }
    }
}
