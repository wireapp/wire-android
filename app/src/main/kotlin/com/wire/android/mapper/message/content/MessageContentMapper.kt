package com.wire.android.mapper.message.content

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.User
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MessageContentMapper @Inject constructor(
    private val systemMessageContentMapper: SystemMessageContentMapper,
    private val regularMessageContentMapper: RegularMessageContentMapper,
    private val dispatchers: DispatcherProvider
) {
    suspend fun fromMessage(
        message: Message.Standalone,
        userList: List<User>
    ): UIMessageContent? {
        return withContext(dispatchers.default()) {
            when (message.visibility) {
                Message.Visibility.VISIBLE ->
                    when (message) {
                        is Message.Regular -> regularMessageContentMapper.mapRegularMessage(
                            message,
                            userList.findUser(message.senderUserId)
                        )

                        is Message.System -> systemMessageContentMapper.mapSystemMessage(message, userList)
                    }

                Message.Visibility.DELETED, // for deleted, there is a state label displayed only
                Message.Visibility.HIDDEN -> null // we don't want to show hidden nor deleted message content in any way
            }
        }
    }

    // TODO: should we keep it here ?
    enum class SelfNameType {
        ResourceLowercase, ResourceTitleCase, NameOrDeleted
    }
}

// TODO: should we keep it here ?
data class MessageResourceProvider(
    @StringRes val memberNameDeleted: Int = R.string.member_name_deleted_label,
    @StringRes val memberNameYouLowercase: Int = R.string.member_name_you_label_lowercase,
    @StringRes val memberNameYouTitlecase: Int = R.string.member_name_you_label_titlecase,
    @StringRes val sentAMessageWithContent: Int = R.string.sent_a_message_with_content
)

