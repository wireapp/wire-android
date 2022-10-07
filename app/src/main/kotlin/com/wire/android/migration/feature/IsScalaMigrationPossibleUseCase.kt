package com.wire.android.migration.feature

import android.content.Context
import com.wire.android.migration.util.ScalaDBNameProvider

class IsScalaMigrationPossibleUseCase(
    private val applicationContext: Context
) {
    operator fun invoke() = applicationContext.getDatabasePath(ScalaDBNameProvider.globalDB()).exists()
}
