package com.wire.android.feature.conversation.content.navigation

import android.app.Activity
import android.content.Intent
import com.wire.android.AndroidTest
import com.wire.android.core.extension.EMPTY
import com.wire.android.feature.conversation.ConversationID
import com.wire.android.feature.conversation.content.ui.ConversationActivity
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ConversationNavigatorTest : AndroidTest() {

    private lateinit var conversationNavigator: ConversationNavigator

    @Before
    fun setUp() {
        conversationNavigator = ConversationNavigator()
    }

    @Test
    fun `given openConversationScreen is called, then opens ConversationActivity`() {
        val activity = mockk<Activity>(relaxed = true)

        conversationNavigator.openConversationScreen(activity, ConversationID.blankID(), String.EMPTY)

        val intentSlot = slot<Intent>()
        verify(exactly = 1) { activity.startActivity(capture(intentSlot)) }
        intentSlot.captured.component?.className shouldBeEqualTo ConversationActivity::class.java.canonicalName
    }
}
