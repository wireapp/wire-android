package com.wire.android.di

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import com.wire.android.navigation.NavigationManager
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
    fun providesApplicationContext(@ApplicationContext appContext: Context) =
        appContext // TODO: provide wrapper instance to easily mock for test
}
