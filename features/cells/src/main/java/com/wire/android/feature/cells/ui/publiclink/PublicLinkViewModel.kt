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
package com.wire.android.feature.cells.ui.publiclink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.ui.common.ActionsViewModel
import com.wire.kalium.cells.domain.model.PublicLink
import com.wire.kalium.cells.domain.usecase.publiclink.CreatePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.DeletePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.GetPublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.SECURE_PUBLIC_LINK_ENABLED
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PublicLinkViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val createPublicLink: CreatePublicLinkUseCase,
    private val getPublicLinkUseCase: GetPublicLinkUseCase,
    private val deletePublicLinkUseCase: DeletePublicLinkUseCase,
    private val fileHelper: FileHelper,
) : ActionsViewModel<PublicLinkViewAction>() {

    private val navArgs: PublicLinkNavArgs = savedStateHandle.navArgs()

    private val _state = MutableStateFlow(
        PublicLinkViewState(
            isEnabled = navArgs.publicLinkId != null,
            isFolder = navArgs.isFolder,
            isLinkAvailable = navArgs.publicLinkId != null,
        )
    )
    internal val state = _state.asStateFlow()
    private var publicLink: PublicLink? = null

    init {

        if (SECURE_PUBLIC_LINK_ENABLED) {
            // Enable settings for UI testing
            _state.update { it.copy(settings = PublicLinkSettings()) }
        }

        navArgs.publicLinkId?.let { linkUuid ->
            loadPublicLink(linkUuid)
        }
    }

    fun onEnabled(enabled: Boolean) {
        if (enabled) {
            if (publicLink == null) {
                createPublicLink()
            }
        } else {
            publicLink?.let {
                deletePublicLink(it.uuid)
            }
        }
    }

    private fun createPublicLink() = viewModelScope.launch {

        _state.update { it.copy(isEnabled = true) }

        createPublicLink(navArgs.assetId, navArgs.fileName)
            .onSuccess { link ->
                publicLink = link
                _state.update {
                    it.copy(
                        isLinkAvailable = true,
                        linkState = PublicLinkState.READY
                    )
                }
            }
            .onFailure {
                sendAction(ShowError(R.string.error_create_public_link))
                _state.update { it.copy(isEnabled = false) }
            }
    }

    private fun loadPublicLink(linkUuid: String) = viewModelScope.launch {
        getPublicLinkUseCase(linkUuid)
            .onSuccess { link ->
                publicLink = link
                _state.update { it.copy(linkState = PublicLinkState.READY) }
            }
            .onFailure {
                sendAction(ShowError(R.string.error_load_public_link, true))
                _state.update { it.copy(linkState = PublicLinkState.ERROR) }
            }
    }

    private fun deletePublicLink(linkUuid: String) = viewModelScope.launch {
        _state.update { it.copy(isEnabled = false) }
        deletePublicLinkUseCase(linkUuid)
            .onSuccess {
                publicLink = null
                _state.update {
                    it.copy(
                        isLinkAvailable = false
                    )
                }
            }
            .onFailure {
                sendAction(ShowError(R.string.error_delete_public_link))
                _state.update { it.copy(isEnabled = true) }
            }
    }

    fun shareLink() {
        publicLink?.url?.let { url ->
            fileHelper.shareUrlChooser(
                url = url,
                onError = {
                    sendAction(ShowError(R.string.error_share_link))
                }
            )
        }
    }

    fun copyLink() {
        publicLink?.url?.let { url ->
            sendAction(CopyLink(url))
        }
    }

    fun onPasswordUpdate() {
        // TODO: Update state
    }

    fun onExpirationUpdate() {
        // TODO: Update state
    }
}

internal data class PublicLinkViewState(
    val isEnabled: Boolean = false,
    val isLinkAvailable: Boolean = false,
    val linkState: PublicLinkState = PublicLinkState.LOADING,
    val isFolder: Boolean = false,
    val isExpired: Boolean = false,
    val settings: PublicLinkSettings? = null,
)

internal enum class PublicLinkState {
    READY, LOADING, ERROR
}

internal data class PublicLinkSettings(
    val passwordSettings: PublicLinkPassword? = null,
    val expirationSettings: PublicLinkExpiration? = null,
)

internal data class PublicLinkPassword(
    val password: String? = null,
)

internal data class PublicLinkExpiration(
    val expiresAt: Long? = null,
)

sealed interface PublicLinkViewAction
internal data class ShowError(val message: Int, val closeScreen: Boolean = false) : PublicLinkViewAction
internal data class CopyLink(val url: String) : PublicLinkViewAction
