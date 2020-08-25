package com.wire.android.core.exception

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

/** Extend this class for UseCase specific failures.*/
abstract class FeatureFailure : Failure()
