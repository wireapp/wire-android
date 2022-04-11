package com.wire.android.di

import android.app.Activity
import android.content.Context
import com.wire.android.util.keyboard.KeyboardInsetsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    @Provides
    fun providesKeyboardInsetsProvider(@ApplicationContext context: Context, activity: Activity) =
        KeyboardInsetsProvider(activity = activity, applicationContext = context)

}
