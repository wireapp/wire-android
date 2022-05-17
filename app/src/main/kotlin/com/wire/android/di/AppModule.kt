package com.wire.android.di

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.FileManager
import com.wire.android.util.FileManagerImpl
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
    fun providesNavigationManager() = NavigationManager()

    @Singleton
    @Provides
    fun providesFileManager(@ApplicationContext appContext: Context): FileManager = FileManagerImpl(appContext)

    @Singleton
    @Provides
    fun providesApplicationContext(@ApplicationContext appContext: Context) = appContext

    @Singleton
    @Provides
    fun providesDefaultDispatchers(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    fun provideDeepLinkProcessor(): DeepLinkProcessor = DeepLinkProcessor()
}
