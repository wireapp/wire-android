package com.wire.android.feature.launch.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.R
import com.wire.android.feature.conversation.list.MainActivity
import com.wire.android.feature.welcome.WelcomeActivity
import org.koin.android.viewmodel.ext.android.viewModel

class LauncherActivity : AppCompatActivity(R.layout.activity_launcher) {

    private val viewModel: LauncherViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigateToNextScreen()
    }

    private fun navigateToNextScreen() {
        if (viewModel.hasActiveUser()) {
            startActivity(MainActivity.newIntent(this))
        } else {
            startActivity(WelcomeActivity.newIntent(this))
        }
        finish()
    }
}
