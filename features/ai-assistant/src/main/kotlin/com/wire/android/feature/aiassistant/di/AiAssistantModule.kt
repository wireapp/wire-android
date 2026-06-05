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
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.feature.aiassistant.di

import com.wire.android.feature.aiassistant.AiEmbeddingModelManager
import com.wire.android.feature.aiassistant.AiModelManager
import com.wire.android.feature.aiassistant.AiMessageComposerAgent
import com.wire.android.feature.aiassistant.DefaultAiEmbeddingModelManager
import com.wire.android.feature.aiassistant.DefaultAiModelManager
import com.wire.android.feature.aiassistant.DefaultAiMessageComposerAgent
import com.wire.android.feature.aiassistant.download.AiModelDownloader
import com.wire.android.feature.aiassistant.download.AiModelHttpClient
import com.wire.android.feature.aiassistant.download.BuildConfigHuggingFaceTokenProvider
import com.wire.android.feature.aiassistant.download.HuggingFaceAiModelDownloader
import com.wire.android.feature.aiassistant.download.HuggingFaceTokenProvider
import com.wire.android.feature.aiassistant.download.UrlConnectionAiModelHttpClient
import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import com.wire.android.feature.aiassistant.model.DefaultAiModelDescriptor
import com.wire.android.feature.aiassistant.storage.AiModelStorage
import com.wire.android.feature.aiassistant.storage.PrivateFileAiModelStorage
import com.wire.android.feature.aiassistant.test.AiModelTestEngine
import com.wire.android.feature.aiassistant.test.DefaultLiteRtLmInferenceFactory
import com.wire.android.feature.aiassistant.test.DefaultMediaPipeLlmInferenceFactory
import com.wire.android.feature.aiassistant.test.LiteRtLmInferenceFactory
import com.wire.android.feature.aiassistant.test.LiteRtLmTestEngine
import com.wire.android.feature.aiassistant.test.MediaPipeLlmInferenceFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
object AiAssistantModule {

    @Provides
    @SingleIn(AppScope::class)
    fun provideAiModelManager(implementation: DefaultAiModelManager): AiModelManager = implementation

    @Provides
    @SingleIn(AppScope::class)
    fun provideAiEmbeddingModelManager(implementation: DefaultAiEmbeddingModelManager): AiEmbeddingModelManager =
        implementation

    @Provides
    @SingleIn(AppScope::class)
    fun provideAiMessageComposerAgent(implementation: DefaultAiMessageComposerAgent): AiMessageComposerAgent =
        implementation

    @Provides
    @SingleIn(AppScope::class)
    fun provideAiModelDownloader(implementation: HuggingFaceAiModelDownloader): AiModelDownloader = implementation

    @Provides
    @SingleIn(AppScope::class)
    fun provideAiModelStorage(implementation: PrivateFileAiModelStorage): AiModelStorage = implementation

    @Provides
    @SingleIn(AppScope::class)
    fun provideAiModelHttpClient(implementation: UrlConnectionAiModelHttpClient): AiModelHttpClient = implementation

    @Provides
    @SingleIn(AppScope::class)
    fun provideHuggingFaceTokenProvider(implementation: BuildConfigHuggingFaceTokenProvider): HuggingFaceTokenProvider =
        implementation

    @Provides
    @SingleIn(AppScope::class)
    fun provideAiModelTestEngine(implementation: LiteRtLmTestEngine): AiModelTestEngine = implementation

    @Provides
    @SingleIn(AppScope::class)
    fun provideLiteRtLmInferenceFactory(implementation: DefaultLiteRtLmInferenceFactory): LiteRtLmInferenceFactory =
        implementation

    @Provides
    @SingleIn(AppScope::class)
    fun provideMediaPipeLlmInferenceFactory(implementation: DefaultMediaPipeLlmInferenceFactory): MediaPipeLlmInferenceFactory =
        implementation

    @Provides
    @SingleIn(AppScope::class)
    fun provideAiModelDescriptors(): List<AiModelDescriptor> = DefaultAiModelDescriptor.allModels
}
