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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.di.CurrentAccount
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getTempWritableAttachmentUri
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class SelfQRCodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val context: Context,
    @CurrentAccount private val selfUserId: UserId,
    private val selfServerLinks: SelfServerConfigUseCase,
    private val kaliumFileSystem: KaliumFileSystem,
    private val dispatchers: DispatcherProvider,
    private val analyticsManager: AnonymousAnalyticsManager
) : ViewModel() {
    private val selfQrCodeNavArgs: SelfQrCodeNavArgs = savedStateHandle.navArgs()
    var selfQRCodeState by mutableStateOf(
        SelfQRCodeState(
            selfUserId,
            handle = selfQrCodeNavArgs.userHandle,
            isTeamMember = selfQrCodeNavArgs.isTeamMember
        )
    )
        private set
    private val cachePath: Path
        get() = kaliumFileSystem.rootCachePath

    init {
        viewModelScope.launch {
            getServerLinks()
        }
    }

    suspend fun shareQRAsset(bitmap: Bitmap): Uri {
        val job = viewModelScope.async {
            val qrImageFile = getTempWritableQRUri(cachePath)
            withContext(dispatchers.io()) {
                context.contentResolver.openFileDescriptor(qrImageFile, "rwt")?.use { fileDescriptor ->
                    FileOutputStream(fileDescriptor.fileDescriptor)
                        .use { fileOutputStream ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, QR_QUALITY_COMPRESSION, fileOutputStream)
                            fileOutputStream.flush()
                        }.also {
                            appLogger.withTextTag("SelfQRCodeViewModel").d("Image written to: $qrImageFile")
                        }
                }
            }
            qrImageFile
        }
        return job.await()
    }

    fun trackAnalyticsEvent(event: AnalyticsEvent.QrCode.Modal) {
        analyticsManager.sendEvent(event)
    }

    private suspend fun getTempWritableQRUri(tempCachePath: Path): Uri = withContext(dispatchers.io()) {
        val tempImagePath = "$tempCachePath/$TEMP_SELF_QR_FILENAME".toPath()
        return@withContext getTempWritableAttachmentUri(context, tempImagePath)
    }

    private suspend fun getServerLinks() {
        selfQRCodeState =
            when (val result = selfServerLinks()) {
                is SelfServerConfigUseCase.Result.Failure -> selfQRCodeState.copy(hasError = true)
                is SelfServerConfigUseCase.Result.Success -> generateSelfUserUrls(result.serverLinks.links.accounts)
            }
    }

    private fun generateSelfUserUrls(accountsUrl: String): SelfQRCodeState =
        selfQRCodeState.copy(
            userAccountProfileLink = String.format(BASE_USER_PROFILE_URL, accountsUrl, selfUserId),
            userProfileLink = String.format(DIRECT_BASE_USER_PROFILE_URL, selfUserId.domain, selfUserId.value)
        )

    companion object {
        const val TEMP_SELF_QR_FILENAME = "temp_self_qr.jpg"
        const val BASE_USER_PROFILE_URL = "%s/user-profile/?id=%s"

        const val DIRECT_BASE_USER_PROFILE_URL = "wire://user/%s/%s"
        const val QR_QUALITY_COMPRESSION = 80
    }
}
