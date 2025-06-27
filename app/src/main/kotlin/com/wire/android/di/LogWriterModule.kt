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

package com.wire.android.di

import android.content.Context
import com.wire.android.BuildConfig
import com.wire.android.util.logging.LogFileWriterV1Impl
   import com.wire.android.util.logging.LogFileWriter
import com.wire.android.util.logging.LogFileWriterV2Impl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class LogWriterModule {

    @Singleton
    @Provides
    fun provideKaliumFileWriter(@ApplicationContext context: Context): LogFileWriter {
        if (BuildConfig.USE_ASYNC_FLUSH_LOGGING) {
            val logsDirectory = LogFileWriter.logsDirectory(context)
            return LogFileWriterV2Impl(logsDirectory)
        } else {
            val logsDirectory = LogFileWriter.logsDirectory(context)
            return LogFileWriterV1Impl(logsDirectory)
        }
    }
}
