/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.OpenResultRecipient
import com.ramcosta.composedestinations.result.ResultRecipient
import com.ramcosta.composedestinations.spec.TypedDestinationSpec

// In v2, DestinationSpec is now TypedDestinationSpec
@SuppressLint("ComposeNamingUppercase")
object PreviewResultRecipient : ResultRecipient<TypedDestinationSpec<Unit>, String> {

    @Composable
    override fun onNavResult(
        listener: @DisallowComposableCalls (NavResult<String>) -> Unit
    ) {
        // fake result
    }

    @Composable
    override fun onNavResult(
        deliverResultOn: OpenResultRecipient.DeliverResultOn,
        listener: @DisallowComposableCalls ((NavResult<String>) -> Unit)
    ) {
        // fake result
    }
}
