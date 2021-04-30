package com.wire.android.feature.auth.client.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.R
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.dialog.DialogBuilder
import com.wire.android.core.ui.dialog.GeneralErrorMessage
import com.wire.android.core.ui.navigation.Navigator
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class DeviceLimitActivity : AppCompatActivity(R.layout.activity_device_limit) {

    private val viewModel by viewModel<DeviceLimitViewModel>()
    private val navigator: Navigator by inject()
    private val dialogBuilder: DialogBuilder by inject()

    private var userId: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initUserIdArg()
        observeOnClientRegistration()
        observeUserSession()
        registerClient()
    }

    private fun observeUserSession() {
        viewModel.isDormantSessionCurrentLiveData.observe(this) {
            it?.onSuccess { navigator.main.openMainScreen(this) }
            it?.onFailure { showErrorDialog() }
        }
    }

    private fun initUserIdArg() {
        userId = intent.getStringExtra(ARG_USER_ID)
    }

    private fun registerClient() {
        viewModel.registerClient("")
    }

    private fun observeOnClientRegistration() {
        viewModel.registerClientLiveData.observe(this) { either ->
            either.onSuccess {
                userId?.let { viewModel.setDormantSessionToCurrent(it) }
            }
            either.onFailure { navigator.login.openDeviceLimitFragment(this) }
        }
    }

    private fun showErrorDialog() = dialogBuilder.showErrorDialog(this, GeneralErrorMessage)

    companion object {
        fun newIntent(context: Context, userId: String) = Intent(context, DeviceLimitActivity::class.java).apply {
            putExtra(ARG_USER_ID, userId)
        }

        private const val ARG_USER_ID = "user_id"
    }
}
