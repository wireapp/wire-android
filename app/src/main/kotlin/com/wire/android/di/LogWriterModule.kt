package com.wire.android.di

import android.content.Context
import com.wire.android.util.KaliumFileWriter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class LogWriterModule {

    @Singleton
    @Provides
    fun kaliumFileWriterProvider(@ApplicationContext context: Context): KaliumFileWriter {
        val logsDirectory = File(context.cacheDir, "logs")
        return KaliumFileWriter(logsDirectory)
    }

}
