package com.wire.android.migration.util

import android.content.Context
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScalaCryptoBoxDirectoryProvider @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) {
    fun rootDir(): File = File(applicationContext.filesDir, SCALA_CRYPTO_BOX_DIR_NAME)
    fun userDir(userId: UserId): File = File(rootDir(), userId.value)

    companion object {
        private const val SCALA_CRYPTO_BOX_DIR_NAME = "otr"
    }
}
