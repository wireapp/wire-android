package com.wire.android.feature.launch.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.R
import com.wire.android.core.ui.navigation.Navigator
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class LauncherActivity : AppCompatActivity(R.layout.activity_launcher) {

    private val viewModel: LauncherViewModel by viewModel()

    private val navigator: Navigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeCurrentSession()
        viewModel.checkIfCurrentSessionExists()
    }

    private fun observeCurrentSession() {
        viewModel.currentSessionExistsLiveData.observe(this) {
            navigateToNextScreen(it)
        }
    }

    private fun navigateToNextScreen(sessionExists: Boolean) {
        if (sessionExists) navigator.main.openMainScreen(this)
        else navigator.welcome.openWelcomeScreen(this)
        finish()
    }
}
