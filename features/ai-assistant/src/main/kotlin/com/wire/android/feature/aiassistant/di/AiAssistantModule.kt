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

import com.wire.android.feature.aiassistant.AiModelManager
import com.wire.android.feature.aiassistant.DefaultAiModelManager
import com.wire.android.feature.aiassistant.download.AiModelDownloader
import com.wire.android.feature.aiassistant.download.AiModelHttpClient
import com.wire.android.feature.aiassistant.download.HuggingFaceAiModelDownloader
import com.wire.android.feature.aiassistant.download.HuggingFaceTokenProvider
import com.wire.android.feature.aiassistant.download.NoOpHuggingFaceTokenProvider
import com.wire.android.feature.aiassistant.download.UrlConnectionAiModelHttpClient
import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import com.wire.android.feature.aiassistant.model.DefaultAiModelDescriptor
import com.wire.android.feature.aiassistant.storage.AiModelStorage
import com.wire.android.feature.aiassistant.storage.PrivateFileAiModelStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiAssistantModule {

    @Binds
    @Singleton
    abstract fun bindAiModelManager(implementation: DefaultAiModelManager): AiModelManager

    @Binds
    @Singleton
    abstract fun bindAiModelDownloader(implementation: HuggingFaceAiModelDownloader): AiModelDownloader

    @Binds
    @Singleton
    abstract fun bindAiModelStorage(implementation: PrivateFileAiModelStorage): AiModelStorage

    @Binds
    @Singleton
    abstract fun bindAiModelHttpClient(implementation: UrlConnectionAiModelHttpClient): AiModelHttpClient

    companion object {
        @Provides
        @Singleton
        fun provideDefaultAiModelDescriptor(): AiModelDescriptor = DefaultAiModelDescriptor.gemma3nE2bIt

        @Provides
        @Singleton
        fun provideHuggingFaceTokenProvider(): HuggingFaceTokenProvider = NoOpHuggingFaceTokenProvider()
    }
}
