package com.wire.android.migration.failure

import com.wire.kalium.logic.CoreFailure

sealed class MigrationFailure: CoreFailure.FeatureFailure() {
    object InvalidRefreshToken: MigrationFailure()
    object ClientNotRegistered: MigrationFailure()
}
