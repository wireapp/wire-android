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

import android.app.NotificationManager
import android.content.Context
import android.location.Geocoder
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.core.app.NotificationManagerCompat
import com.wire.android.BuildConfig
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.feature.aiassistant.AiModelSelectionStore
import com.wire.android.feature.aiassistant.GlobalDataStoreAiModelSelectionStore
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.mapper.MessageResourceProvider
import com.wire.android.ui.analytics.AnalyticsConfiguration
import com.wire.android.ui.home.conversations.MessageSharedState
import com.wire.android.ui.home.messagecomposer.location.LocationPickerParameters
import com.wire.android.util.CurrentTimeProvider
import com.wire.android.util.GetMediaMetadataUseCase
import com.wire.android.util.GetMediaMetadataUseCaseImpl
import com.wire.android.util.dispatchers.DefaultDispatcherProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.AndroidUiTextResolver
import com.wire.android.util.ui.UiTextResolver
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.SingleIn

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentAppVersion

@BindingContainer
@Suppress("TooManyFunctions")
object AppModule {

    @CurrentAppVersion
    @Provides
    fun provideCurrentAppVersion(): Int = BuildConfig.VERSION_CODE

    @SingleIn(AppScope::class)
    @Provides
    fun providesApplicationContext(@ApplicationContext appContext: Context): Context = appContext

    @SingleIn(AppScope::class)
    @Provides
    fun provideDefaultDispatchers(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    fun provideMessageResourceProvider(): MessageResourceProvider = MessageResourceProvider()

    @SingleIn(AppScope::class)
    @Provides
    fun provideUiTextResolver(@ApplicationContext appContext: Context): UiTextResolver =
        AndroidUiTextResolver(appContext)

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

    @SingleIn(AppScope::class)
    @Provides
    fun provideCurrentTimeProvider(): CurrentTimeProvider = CurrentTimeProvider.Default

    @Provides
    fun provideGeocoder(appContext: Context): Geocoder = Geocoder(appContext)

    @Provides
    fun provideLocationPickerParameters(): LocationPickerParameters = LocationPickerParameters()

    @Provides
    fun provideAnalyticsConfiguration(): AnalyticsConfiguration =
        if (BuildConfig.ANALYTICS_ENABLED) AnalyticsConfiguration.Enabled else AnalyticsConfiguration.Disabled

    @Provides
    fun provideAnonymousAnalyticsManager(): AnonymousAnalyticsManager = AnonymousAnalyticsManagerImpl

    @Provides
    fun provideAudioManager(@ApplicationContext context: Context): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Provides
    @Named("useNewLoginForDefaultBackend")
    fun provideUseNewLoginForDefaultBackend(): Boolean = BuildConfig.USE_NEW_LOGIN_FOR_DEFAULT_BACKEND

    @Provides
    @SingleIn(AppScope::class)
    fun provideMessageSharedState(): MessageSharedState = MessageSharedState()

    @Provides
    fun provideGetMediaMetadataUseCase(): GetMediaMetadataUseCase = GetMediaMetadataUseCaseImpl()

    @Singleton
    @Provides
    fun provideAiModelSelectionStore(store: GlobalDataStore): AiModelSelectionStore = GlobalDataStoreAiModelSelectionStore(store)
}
