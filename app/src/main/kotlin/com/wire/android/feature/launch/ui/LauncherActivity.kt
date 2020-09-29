package com.wire.android.feature.launch.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
import com.wire.android.R
import com.wire.android.feature.conversation.list.MainActivity
import com.wire.android.feature.welcome.WelcomeActivity
import org.koin.android.viewmodel.ext.android.viewModel

class LauncherActivity : AppCompatActivity(R.layout.activity_launcher) {

    private val viewModel: LauncherViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeCurrentSession()
        viewModel.checkCurrentSessionExists()
    }

    private fun observeCurrentSession() {
        viewModel.hasCurrentSessionLiveData.observe(this) {
            navigateToNextScreen(it)
        }
    }

    private fun navigateToNextScreen(hasCurrentUser: Boolean) {
        val nextIntent = if (hasCurrentUser) MainActivity.newIntent(this) else WelcomeActivity.newIntent(this)
        startActivity(nextIntent)
        finish()
    }
}
