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
package com.wire.android.ui.userprofile.qr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.CurrentAccount
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class SelfQRCodeViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val context: Context,
        @CurrentAccount private val selfUserId: UserId,
        private val selfServerLinks: SelfServerConfigUseCase
    ) : ViewModel() {
        private val selfQrCodeNavArgs: SelfQrCodeNavArgs = savedStateHandle.navArgs()
        var selfQRCodeState by mutableStateOf(SelfQRCodeState(selfUserId, handle = selfQrCodeNavArgs.handle))
            private set

        init {
            viewModelScope.launch {
                getServerLinks()
            }
        }

        suspend fun shareQRAsset(bitmap: Bitmap): Uri {
            val qrImageFile = tempQRUri(context)
            viewModelScope
                .launch {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openFileDescriptor(qrImageFile, "rwt")?.use { fileDescriptor ->
                            FileOutputStream(fileDescriptor.fileDescriptor)
                                .use { fileOutputStream ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, QR_QUALITY_COMPRESSION, fileOutputStream)
                                    fileOutputStream.flush()
                                }.also {
                                    Log.d("SelfQRCodeViewModel", "Image written to: $qrImageFile")
                                }
                        }
                    }
                }.join()
            return qrImageFile
        }

        private fun tempQRUri(context: Context): Uri =
            run {
                val tempFile = File.createTempFile("temp_qr", ".jpg", context.cacheDir)
                tempFile.deleteOnExit()
                tempFile.toUri()
            }

        private suspend fun getServerLinks() {
            selfQRCodeState =
                when (val result = selfServerLinks()) {
                    is SelfServerConfigUseCase.Result.Failure -> selfQRCodeState.copy(hasError = true)
                    is SelfServerConfigUseCase.Result.Success -> generateSelfUserUrl(result.serverLinks.links.accounts)
                }
        }

        private fun generateSelfUserUrl(accountsUrl: String): SelfQRCodeState =
            selfQRCodeState.copy(
                userProfileLink = String.format(BASE_USER_PROFILE_URL, accountsUrl, selfUserId.value),
            )

        companion object {
            const val BASE_USER_PROFILE_URL = "%s/user-profile/?id=%s"
            // This URL, can be used when we have a direct link to user profile Milestone2
            const val DIRECT_BASE_USER_PROFILE_URL = "wire://user/%s"
            const val QR_QUALITY_COMPRESSION = 80
        }
    }
