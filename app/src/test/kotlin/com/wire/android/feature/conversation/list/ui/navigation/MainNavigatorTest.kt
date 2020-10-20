package com.wire.android.feature.conversation.list.ui.navigation

import android.content.Context
import android.content.Intent
import com.wire.android.AndroidTest
import com.wire.android.capture
import com.wire.android.feature.conversation.list.MainActivity
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify

class MainNavigatorTest : AndroidTest() {

    @Mock
    private lateinit var context: Context

    @Captor
    private lateinit var intentCaptor: ArgumentCaptor<Intent>

    private lateinit var mainNavigator: MainNavigator

    @Before
    fun setUp() {
        mainNavigator = MainNavigator()
    }

    @Test
    fun `given openMainScreen is called, then opens MainActivity and clears stack`() {
        mainNavigator.openMainScreen(context)

        verify(context).startActivity(capture(intentCaptor))
        intentCaptor.value.let {
            assertThat(it.component?.className).isEqualTo(MainActivity::class.java.canonicalName)
            assertThat(it.extras).isNull()
            assertThat(it.flags or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(it.flags)
        }
    }
}
