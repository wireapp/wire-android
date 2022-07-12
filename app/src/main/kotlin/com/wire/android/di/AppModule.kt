package com.wire.android.di

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import com.wire.android.mapper.MessageResourceProvider
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.FileManager
import com.wire.android.util.deeplink.DeepLinkProcessor
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
    fun provideFileManager(@ApplicationContext appContext: Context): FileManager = FileManager(appContext)

    @Singleton
    @Provides
    fun provideApplicationContext(@ApplicationContext appContext: Context) = appContext

    @Singleton
    @Provides
    fun provideDefaultDispatchers(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    fun provideDeepLinkProcessor(): DeepLinkProcessor = DeepLinkProcessor()

    @Provides
    fun provideMessageResourceProvider(): MessageResourceProvider = MessageResourceProvider()
}
