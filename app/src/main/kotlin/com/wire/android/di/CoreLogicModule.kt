package com.wire.android.di

import android.content.Context
import com.wire.android.util.DeviceLabel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.auth.LoginUseCase
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Singleton


@Module
@InstallIn(ViewModelComponent::class)
class CoreLogicModule {

    @ViewModelScoped
    @Provides
    fun coreLogicProvider(@ApplicationContext context: Context): CoreLogic {
        val proteusPath = context.getDir("proteus", Context.MODE_PRIVATE).path
        val deviceLabel = DeviceLabel.label
        return CoreLogic(applicationContext = context, rootProteusDirectoryPath = proteusPath, clientLabel = deviceLabel)
    }

    @ViewModelScoped
    @Provides
    fun loginUseCaseProvider(coreLogic: CoreLogic): LoginUseCase = coreLogic.getAuthenticationScope().loginUsingEmail
}
