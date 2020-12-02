package com.wire.android.feature.conversation.data

import com.wire.android.InstrumentationTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("WIP")
@Suppress("EmptyFunctionBlock")
class ConversationDataSourceInstrumentationTest : InstrumentationTest() {

    @Before
    fun setUp() {
    }

    @Test
    fun givenWeHaveNoConversationsLocally_thenMakesRemoteCall_andReturnsFetchedItems() {

    }

    @Test
    fun givenWeHaveConversationsLocally_thenDoesNotMakeRemoteCall_andReturnsLocalItems() {

    }

    @Test
    fun givenRemoteCallIsMade_whenCallFails_propagatesFailure() {

    }
}
