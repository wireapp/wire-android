package com.wire.android.feature.conversation.data.remote

import com.wire.android.UnitTest
import com.wire.android.framework.network.connectedNetworkHandler
import org.junit.Before

//TODO complete when authentication goes in
class ConversationRemoteDataSourceTest : UnitTest() {

    private lateinit var remoteDataSource: ConversationRemoteDataSource

    @Before
    fun setup() {
        remoteDataSource = ConversationRemoteDataSource(connectedNetworkHandler)
    }
}
