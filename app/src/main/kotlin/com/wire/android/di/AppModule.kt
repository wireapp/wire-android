package com.wire.android.di

import androidx.compose.material3.ExperimentalMaterial3Api
import com.wire.android.navigation.NavigationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @ExperimentalMaterial3Api
    @Singleton
    @Provides
    fun providesNavigationManager() = NavigationManager()
}
