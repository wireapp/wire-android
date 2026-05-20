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
package com.wire.android.ui.home.messagecomposer.recordaudio

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.media.audiomessage.AudioFocusHelper
import com.wire.android.media.audiomessage.RecordAudioMessagePlayer
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.asset.AudioNormalizedLoudnessBuilder
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class RecordAudioViewModelFactory(
    private val recordAudioMessagePlayer: RecordAudioMessagePlayer,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase,
    private val recordAudioFileGateway: RecordAudioFileGateway,
    private val currentScreenManager: CurrentScreenManager,
    private val audioMediaRecorder: AudioMediaRecorder,
    private val globalDataStore: GlobalDataStore,
    private val audioNormalizedLoudnessBuilder: AudioNormalizedLoudnessBuilder,
    private val audioFocusHelper: AudioFocusHelper,
    private val dispatchers: DispatcherProvider,
    private val kaliumFileSystem: KaliumFileSystem,
) {
    fun create(): RecordAudioViewModel = RecordAudioViewModel(
        recordAudioMessagePlayer = recordAudioMessagePlayer,
        observeEstablishedCalls = observeEstablishedCalls,
        getAssetSizeLimit = getAssetSizeLimit,
        recordAudioFileGateway = recordAudioFileGateway,
        currentScreenManager = currentScreenManager,
        audioMediaRecorder = audioMediaRecorder,
        globalDataStore = globalDataStore,
        audioNormalizedLoudnessBuilder = audioNormalizedLoudnessBuilder,
        audioFocusHelper = audioFocusHelper,
        dispatchers = dispatchers,
        kaliumFileSystem = kaliumFileSystem,
    )
}
