package com.wire.android.feature.welcome.ui.navigation

import android.content.Context
import android.content.Intent
import com.wire.android.AndroidTest
import com.wire.android.feature.welcome.ui.WelcomeActivity
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class WelcomeNavigatorTest : AndroidTest() {

    @MockK
    private lateinit var context: Context

    private lateinit var welcomeNavigator: WelcomeNavigator

    @Before
    fun setUp() {
        welcomeNavigator = WelcomeNavigator()
    }

    @Test
    fun `given openWelcomeScreen is called, then opens WelcomeActivity`() {
        welcomeNavigator.openWelcomeScreen(context)

        val intentSlot = slot<Intent>()
        verify(exactly = 1) { context.startActivity(capture(intentSlot)) }
        intentSlot.captured.let {
            it.component?.className shouldBeEqualTo WelcomeActivity::class.java.canonicalName
            it.extras shouldBe null
        }
    }
}
