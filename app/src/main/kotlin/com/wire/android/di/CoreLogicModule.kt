package com.wire.android.di

import android.content.Context
import com.wire.android.util.DeviceLabel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.wire.kalium.logic.CoreLogic
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Qualifier
import javax.inject.Singleton
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.runBlocking
import java.lang.IllegalStateException

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KaliumCoreLogic

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentSession

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

    @CurrentSession
    @ViewModelScoped
    @Provides
    // TODO: can be improved by caching the current session in kalium or changing the scope to ActivityRetainedScoped
    fun currentSessionProvider(@KaliumCoreLogic coreLogic: CoreLogic): AuthSession {
        return runBlocking {
            return@runBlocking when(val result = coreLogic.getAuthenticationScope().session.currentSession.invoke()) {
                is CurrentSessionResult.Success -> result.authSession
                else -> {throw IllegalStateException("no current session where found")}
            }
        }
    }

    @ViewModelScoped
    @Provides
    fun loginUseCaseProvider(@KaliumCoreLogic coreLogic: CoreLogic) = coreLogic.getAuthenticationScope().login

    @ViewModelScoped
    @Provides
    fun registerClient(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentSession currentSession: AuthSession
    ) = coreLogic.getSessionScope(currentSession).client.register

}
