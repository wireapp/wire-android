package com.wire.android.feature.welcome.ui.navigation

import android.content.Context
import android.content.Intent
import com.wire.android.AndroidTest
import com.wire.android.capture
import com.wire.android.feature.welcome.ui.WelcomeActivity
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify

class WelcomeNavigatorTest : AndroidTest() {

    @Mock
    private lateinit var context: Context

    @Captor
    private lateinit var intentCaptor: ArgumentCaptor<Intent>

    private lateinit var welcomeNavigator: WelcomeNavigator

    @Before
    fun setUp() {
        welcomeNavigator = WelcomeNavigator()
    }

    @Test
    fun `given openWelcomeScreen is called, then opens WelcomeActivity`() {
        welcomeNavigator.openWelcomeScreen(context)

        verify(context).startActivity(capture(intentCaptor))
        intentCaptor.value.let {
            assertThat(it.component?.className).isEqualTo(WelcomeActivity::class.java.canonicalName)
            assertThat(it.extras).isNull()
        }
    }
}
