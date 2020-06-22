package com.wire.android.core.exception

/**
 * Base Class for handling errors/failures/exceptions.
 * Every feature specific failure should extend [FeatureFailure] class.
 */
sealed class Failure

sealed class NetworkFailure : Failure()
object Conflict : NetworkFailure()
object Forbidden : NetworkFailure()
object NetworkConnection : NetworkFailure()

data class ObservableFailure(val throwable: Throwable) : Failure()

/** Extend this class for UseCase specific failures.*/
abstract class FeatureFailure : Failure()
