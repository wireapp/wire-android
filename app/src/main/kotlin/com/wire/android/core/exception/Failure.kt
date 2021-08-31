package com.wire.android.core.exception

import android.database.sqlite.SQLiteException

/**
 * Base Class for handling errors/failures/exceptions.
 * Every feature specific failure should extend [FeatureFailure] class.
 */
sealed class Failure

sealed class NetworkFailure : Failure()
object NetworkConnection : NetworkFailure()
object ServerError : NetworkFailure()
object BadRequest : NetworkFailure()
object Unauthorized : NetworkFailure()
object Forbidden : NetworkFailure()
object NotFound : NetworkFailure()
object Cancelled : NetworkFailure()
object InternalServerError : NetworkFailure()
object Conflict : NetworkFailure()
object TooManyRequests : NetworkFailure()
object EmptyResponseBody : NetworkFailure()

sealed class DatabaseRequestFailure : Failure()
data class SQLiteFailure(val reason: SQLiteException? = null) : DatabaseRequestFailure()
data class DatabaseFailure(val reason: Exception? = null) : DatabaseRequestFailure()
object NoEntityFound : DatabaseRequestFailure()

sealed class IOFailure : Failure()
data class GeneralIOFailure(val reason: Exception? = null) : IOFailure()
object FileDoesNotExist : IOFailure()
object IOAccessDenied : IOFailure()

sealed class CryptoBoxFailure : Failure()
object InitializationFailure : CryptoBoxFailure()
object SessionNotFound : CryptoBoxFailure()
object MessageAlreadyDecrypted : CryptoBoxFailure()
class UnknownCryptoFailure(val cause: Throwable? = null) : CryptoBoxFailure()

sealed class CacheRequestFailure : Failure()
object CacheFailure : CacheRequestFailure()

/** Extend this class for UseCase specific failures.*/
abstract class FeatureFailure : Failure()
