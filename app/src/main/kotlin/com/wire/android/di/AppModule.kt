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

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.core.app.NotificationManagerCompat
import com.wire.android.BuildConfig
import com.wire.android.mapper.MessageResourceProvider
import com.wire.android.util.dispatchers.DefaultDispatcherProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentAppVersion

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @CurrentAppVersion
    @Provides
    fun provideCurrentAppVersion(): Int = BuildConfig.VERSION_CODE

    @Singleton
    @Provides
    fun providesApplicationContext(@ApplicationContext appContext: Context) = appContext

    @Singleton
    @Provides
    fun provideDefaultDispatchers(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    fun provideMessageResourceProvider(): MessageResourceProvider = MessageResourceProvider()

    @Provides
    fun provideNotificationManagerCompat(appContext: Context): NotificationManagerCompat =
        NotificationManagerCompat.from(appContext)

    @Provides
    fun provideNotificationManager(appContext: Context): NotificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Provides
    fun provideMusicMediaPlayer(): MediaPlayer {
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }
}
