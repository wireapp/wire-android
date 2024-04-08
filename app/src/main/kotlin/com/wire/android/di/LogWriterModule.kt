/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.di

import android.content.Context
import com.wire.android.util.LogFileWriter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LogFileDirectory

@Module
@InstallIn(SingletonComponent::class)
class LogWriterModule {

    @Singleton
    @Provides
    fun provideKaliumFileWriter(@ApplicationContext context: Context): LogFileWriter {
        val logsDirectory = LogFileWriter.logsDirectory(context)
        return LogFileWriter(logsDirectory)
    }
}

@Module
@InstallIn(ViewModelComponent::class)
class LogFileDirectoryModule {
    @LogFileDirectory
    @ViewModelScoped
    @Provides
    fun provideLogFileDirectory(@ApplicationContext context: Context): File {
        return LogFileWriter.logsDirectory(context)
    }
}
