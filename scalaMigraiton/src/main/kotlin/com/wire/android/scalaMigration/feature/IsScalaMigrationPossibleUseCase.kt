package com.wire.android.scalaMigration.feature

import android.content.Context
import com.wire.android.scalaMigration.util.ScalaDBNameProvider

class IsScalaMigrationPossibleUseCase(
    private val applicationContext: Context
) {
    operator fun invoke() = applicationContext.getDatabasePath(ScalaDBNameProvider.globalDB()).exists()
}
