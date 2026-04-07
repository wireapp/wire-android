/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.sharing

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.wire.android.appLogger
import com.wire.android.ui.BaseActivity
import com.wire.android.ui.WireActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IncomingShareActivity : BaseActivity() {
    private val viewModel: IncomingShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            when (val result = viewModel.stageShare(intent)) {
                is StageIncomingShareUseCase.Result.Success -> {
                    startActivity(
                        Intent(this@IncomingShareActivity, WireActivity::class.java).apply {
                            action = IncomingShareIntents.ACTION_OPEN_IMPORTED_SHARE
                            putExtra(IncomingShareIntents.EXTRA_IMPORT_SESSION_ID, result.importSessionId)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                    )
                }

                StageIncomingShareUseCase.Result.Failure.InvalidContent ->
                    appLogger.w("Failed to stage incoming share: invalid content")

                StageIncomingShareUseCase.Result.Failure.NoSupportedContent ->
                    appLogger.w("Failed to stage incoming share: no supported content")
            }
            finish()
        }
    }
}
