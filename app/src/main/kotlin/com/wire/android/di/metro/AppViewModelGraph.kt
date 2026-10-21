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
package com.wire.android.di.metro

import com.wire.android.feature.cells.ui.CellsMetroViewModelBindings
import com.wire.android.feature.meetings.ui.MeetingsManualViewModelFactoryMetroBindings
import com.wire.android.feature.meetings.ui.MeetingsMetroViewModelBindings
import com.wire.android.feature.sketch.SketchMetroViewModelBindings
import com.wire.android.pdfviewer.PdfViewerManualViewModelFactoryMetroBindings
import com.wire.android.search.SearchManualViewModelFactoryMetroBindings
import com.wire.android.ui.calling.CallingManualViewModelFactoryMetroBindings
import com.wire.android.ui.calling.CallingMetroViewModelBindings
import com.wire.android.ui.common.CommonManualViewModelFactoryMetroBindings
import com.wire.android.ui.common.CommonMetroViewModelBindings
import com.wire.android.ui.common.CoreUICommonManualViewModelFactoryMetroBindings
import com.wire.android.ui.debug.DebugInfoManualViewModelFactoryMetroBindings
import com.wire.android.ui.debug.DebugMetroViewModelBindings
import com.wire.android.ui.home.HomeMetroViewModelBindings
import com.wire.android.ui.home.conversations.ConversationCoreManualViewModelFactoryMetroBindings
import com.wire.android.ui.home.conversations.ConversationDetailsManualViewModelFactoryMetroBindings
import com.wire.android.ui.home.conversations.ConversationSearchFolderManualViewModelFactoryMetroBindings
import com.wire.android.ui.home.conversations.ConversationSearchFolderMetroViewModelBindings
import com.wire.android.ui.home.settings.SettingsManualViewModelFactoryMetroBindings
import com.wire.android.videoplayer.VideoPlayerManualViewModelFactoryMetroBindings
import com.wire.android.audioplayer.AudioPlayerManualViewModelFactoryMetroBindings
import com.wire.kalium.logic.di.KaliumConsumer
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageByIdUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.MetroViewModelMultibindings
import dev.zacsweers.metrox.viewmodel.ViewModelScope

/** One Android construction graph for one ViewModel, extending the retained session graph. */
@GraphExtension(
    ViewModelScope::class,
    bindingContainers = [
        WireMetroViewModelBindings::class,
        DebugInfoManualViewModelFactoryMetroBindings::class,
        DebugMetroViewModelBindings::class,
        HomeMetroViewModelBindings::class,
        ConversationSearchFolderMetroViewModelBindings::class,
        ConversationSearchFolderManualViewModelFactoryMetroBindings::class,
        ConversationCoreManualViewModelFactoryMetroBindings::class,
        ConversationDetailsManualViewModelFactoryMetroBindings::class,
        SettingsManualViewModelFactoryMetroBindings::class,
        CallingManualViewModelFactoryMetroBindings::class,
        CallingMetroViewModelBindings::class,
        CommonManualViewModelFactoryMetroBindings::class,
        CommonMetroViewModelBindings::class,
        CellsMetroViewModelBindings::class,
        MeetingsMetroViewModelBindings::class,
        MeetingsManualViewModelFactoryMetroBindings::class,
        SketchMetroViewModelBindings::class,
        CoreUICommonManualViewModelFactoryMetroBindings::class,
        SearchManualViewModelFactoryMetroBindings::class,
        VideoPlayerManualViewModelFactoryMetroBindings::class,
        AudioPlayerManualViewModelFactoryMetroBindings::class,
        PdfViewerManualViewModelFactoryMetroBindings::class,
        KaliumConsumerModule::class,
    ],
)
interface AppViewModelGraph : MetroViewModelMultibindings {
    val consumerViewModelFactory: WireViewModelGraphFactory

    @GraphExtension.Factory
    interface Factory {
        fun createViewModelGraph(@Provides consumer: KaliumConsumer): AppViewModelGraph
    }
}

/** These bindings exist only in the child; the session must not prebuild either use case. */
@BindingContainer
object KaliumConsumerModule {
    @Provides
    fun observeConversationDetails(consumer: KaliumConsumer): ObserveConversationDetailsUseCase =
        consumer.observeConversationDetails

    @Provides
    fun observeMessageById(consumer: KaliumConsumer): ObserveMessageByIdUseCase =
        consumer.observeMessageById

    @Provides
    fun getMessageById(consumer: KaliumConsumer): GetMessageByIdUseCase =
        consumer.getMessageById
}
