package com.wire.android.feature.auth.client.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.R
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.logger.Logger
import com.wire.android.core.ui.dialog.DialogBuilder
import com.wire.android.core.ui.dialog.GeneralErrorMessage
import com.wire.android.core.ui.navigation.Navigator
import com.wire.android.feature.auth.client.usecase.DevicesLimitReached
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class DeviceLimitActivity : AppCompatActivity(R.layout.activity_device_limit) {

    private val viewModel by viewModel<DeviceLimitViewModel>()
    private val navigator: Navigator by inject()
    private val dialogBuilder: DialogBuilder by inject()
    private val logger: Logger by inject()

    private val userId get() = intent.getStringExtra(ARG_USER_ID)
    private val password get() = intent.getStringExtra(ARG_PASSWORD)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeOnClientRegistration()
        registerClient(userId, password)
    }

    private fun registerClient(userId: String?, password: String?) {
        if (userId != null && password != null) {
            viewModel.registerClient(userId, password)
        } else {
            logger.d(DeviceLimitActivity::class.simpleName, "userId or password is null")
            showErrorDialog()
        }
    }

    private fun observeOnClientRegistration() {
        viewModel.registerClientLiveData.observe(this) { either ->
            either.onSuccess {
                navigator.main.openMainScreen(this)
            }.onFailure {
                if (it is DevicesLimitReached)
                    navigator.login.openDeviceLimitErrorScreen(this)
                else
                    showErrorDialog()
            }

        }
    }

    private fun showErrorDialog() = dialogBuilder.showErrorDialog(this, GeneralErrorMessage)

    companion object {
        fun newIntent(context: Context, userId: String, password: String) = Intent(context, DeviceLimitActivity::class.java).apply {
            putExtra(ARG_USER_ID, userId)
            putExtra(ARG_PASSWORD, password)
        }

        private const val ARG_USER_ID = "user_id"
        private const val ARG_PASSWORD = "password"
    }
}
