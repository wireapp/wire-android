package com.wire.android.feature.conversation.list.ui.navigation

import android.content.Context
import android.content.Intent
import com.wire.android.AndroidTest
import com.wire.android.feature.conversation.list.MainActivity
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class MainNavigatorTest : AndroidTest() {

    private lateinit var mainNavigator: MainNavigator

    @Before
    fun setUp() {
        mainNavigator = MainNavigator()
    }

    @Test
    fun `given openMainScreen is called, then opens MainActivity and clears stack`() {
        val context = mockk<Context>(relaxed = true)

        mainNavigator.openMainScreen(context)

        val intentSlot = slot<Intent>()
        verify(exactly = 1) { context.startActivity(capture(intentSlot)) }
        intentSlot.captured.let {
            it.component?.className shouldBeEqualTo MainActivity::class.java.canonicalName
            it.extras shouldBe null
            it.flags or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK shouldBeEqualTo it.flags
        }
    }
}
