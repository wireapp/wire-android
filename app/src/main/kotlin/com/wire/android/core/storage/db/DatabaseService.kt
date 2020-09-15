package com.wire.android.core.storage.db

import android.database.sqlite.SQLiteException
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NoEntityFound
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.core.functional.Either

interface DatabaseService {

    @Suppress("TooGenericExceptionCaught")
    suspend fun <T> request(default: T? = null, call: suspend () -> T?): Either<Failure, T> = try {
        call()?.let { Either.Right(it) }
            ?: default?.let { Either.Right(it) }
            ?: Either.Left(NoEntityFound)
    } catch (e: SQLiteException) {
        Either.Left(SQLiteFailure(e))
    } catch (e: Exception) {
        Either.Left(DatabaseFailure(e))
    }
}
