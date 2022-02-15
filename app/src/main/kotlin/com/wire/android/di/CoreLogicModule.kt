package com.wire.android.di

import android.content.Context
import com.wire.android.util.DeviceLabel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KaliumCoreLogic

@Module
@InstallIn(SingletonComponent::class)
class CoreLogicModule {

    @KaliumCoreLogic
    @Singleton
    @Provides
    fun coreLogicProvider(@ApplicationContext context: Context): CoreLogic {
        val proteusPath = context.getDir("proteus", Context.MODE_PRIVATE).path
        val deviceLabel = DeviceLabel.label
        return CoreLogic(applicationContext = context, rootProteusDirectoryPath = proteusPath, clientLabel = deviceLabel)
    }
}

@Module
@InstallIn(ViewModelComponent::class)
class UseCaseModule {
    @ViewModelScoped
    @Provides
    fun loginUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) = coreLogic.getAuthenticationScope().login

    @ViewModelScoped
    @Provides
    fun registerClientUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic): (AuthSession) -> RegisterClientUseCase =
        { coreLogic.getSessionScope(it).client.register } //TODO replace when the final solution is ready in Kalium
}
