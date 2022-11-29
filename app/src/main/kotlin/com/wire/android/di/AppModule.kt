package com.wire.android.di

import android.app.NotificationManager
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.app.NotificationManagerCompat
import com.wire.android.mapper.MessageResourceProvider
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.dispatchers.DefaultDispatcherProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @ExperimentalMaterial3Api
    @Singleton
    @Provides
    fun provideNavigationManager() = NavigationManager()

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

}
