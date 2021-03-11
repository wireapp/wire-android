package com.wire.android.shared.asset

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import java.io.InputStream

interface AssetRepository {

    suspend fun publicAsset(key: String): Either<Failure, InputStream>
}
