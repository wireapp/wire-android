package com.wire.android.core.exception

/**
 * Base Class for handling errors/failures/exceptions.
 * Every feature specific failure should extend [FeatureFailure] class.
 */
sealed class Failure

/** Extend this class for UseCase specific failures.*/
abstract class FeatureFailure : Failure()