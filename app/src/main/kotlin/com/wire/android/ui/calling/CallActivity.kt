/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.calling

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wire.android.ui.AppLockActivity
import com.wire.android.ui.common.setupOrientationForDevice
import com.wire.android.util.SwitchAccountObserver
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
abstract class CallActivity : AppCompatActivity() {

    @Inject
    lateinit var switchAccountObserver: SwitchAccountObserver

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_SCREEN_TYPE = "screen_type"
        const val EXTRA_SHOULD_ANSWER_CALL = "should_answer_call"
    }

    private val callActivityViewModel: CallActivityViewModel by viewModels()
    protected val qualifiedIdMapper = QualifiedIdMapperImpl(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupOrientationForDevice()
    }
    fun switchAccountIfNeeded(userId: String?) {
        userId?.let {
            qualifiedIdMapper.fromStringToQualifiedID(it).run {
                callActivityViewModel.switchAccountIfNeeded(userId = this, actions = switchAccountObserver)
            }
        }
    }

    fun openAppLockActivity() {
        Intent(this, AppLockActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }.run {
                startActivity(this)
            }
    }

    fun setUpCallingFlags() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    fun cleanUpCallingFlags() {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    fun setUpScreenshotPreventionFlag() {
        lifecycleScope.launch {
            if (callActivityViewModel.isScreenshotCensoringConfigEnabled().await()) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
