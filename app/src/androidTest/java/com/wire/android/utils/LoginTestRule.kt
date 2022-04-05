package com.wire.android.utils

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.test.core.app.ApplicationProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.ServerConfig
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.junit.rules.ExternalResource

/**
 * JUnit Test Rule that will be executed on every test that needs a session to be in place.
 * To use it, you need to add it to your test as:
 *
 * ```
 * @get:Rule(order = 3) // give it a priority greater than [HiltAndroidRule]
 * var loginTestRule = LoginTestRule()
 * ```
 *
 */
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class,
    ExperimentalCoroutinesApi::class
)
class LoginTestRule : ExternalResource() {

    private val tempProteusFile = File.createTempFile("test_", "_proteus")

    override fun before() {
        val coreLogic = CoreLogic(
            appContext = ApplicationProvider.getApplicationContext(),
            clientLabel = "appTest",
            rootProteusDirectoryPath = tempProteusFile.absolutePath
        )

        TestScope().launch {
            coreLogic.authenticationScope {
                this.login("mustafa+1@wire.com", PASSWORD, false, ServerConfig.STAGING)
            }
        }
    }

    override fun after() {
        tempProteusFile.delete()
    }
}
