package com.wire.android.feature.conversation.list.ui.icon

import com.wire.android.UnitTest
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.conversation.list.ui.ConversationListItem
import com.wire.android.shared.asset.ui.imageloader.AvatarLoader
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class ConversationIconProviderTest : UnitTest() {

    @MockK
    private lateinit var avatarLoader: AvatarLoader

    private lateinit var conversationIconProvider: ConversationIconProvider

    @Before
    fun setUp() {
        conversationIconProvider = ConversationIconProvider(avatarLoader)
    }

    @Test
    fun `given provide is called, when conversation has no members, then returns NoParticipantsConversationIcon`() {
        val conversation = mockk<ConversationListItem>()
        every { conversation.members } returns emptyList()

        val icon = conversationIconProvider.provide(conversation)

        icon shouldBeInstanceOf NoParticipantsConversationIcon::class
    }

    @Test
    fun `given provide is called, when conversation has 1 member, then returns SingleParticipantConversationIcon`() {
        val conversation = mockk<ConversationListItem>()
        every { conversation.members } returns listOf(mockk())

        val icon = conversationIconProvider.provide(conversation)

        icon shouldBeInstanceOf SingleParticipantConversationIcon::class
    }

    @Test
    fun `given provide is called, when conversation has more than one members, then returns GroupConversationIcon`() {
        val conversation = mockk<ConversationListItem>()

        val member1 = mockk<Contact>()
        every { member1.id } returns "id-1"
        val member2 = mockk<Contact>()
        every { member2.id } returns "id-2"

        every { conversation.members } returns listOf(member1, member2)

        val icon = conversationIconProvider.provide(conversation)

        icon shouldBeInstanceOf GroupConversationIcon::class
    }
}
