package com.wire.android.framework.retry

import android.util.Log
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RetryTestRule(val retryCount: Int = 3) : TestRule {

    private val TAG = RetryTestRule::class.java.simpleName

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
            lateinit var caughtThrowable: Throwable

            // implement retry logic here
            for (i in 0 until retryCount) {
                try {
                    base.evaluate()
                    return
                } catch (t: Throwable) {
                    caughtThrowable = t
                    Log.e(TAG, "${description.displayName}: run ${(i + 1)} failed")
                }
            }

            Log.e(TAG, "${description.displayName}: giving up after $retryCount failures")
            throw caughtThrowable
        }
    }
}
