package com.wire.android.framework.retry

import android.util.Log
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RetryTestRule(val retryCount: Int = RETRY_COUNT) : TestRule {

    override fun apply(testStatement: Statement, description: Description): Statement =
        if (retryCount < 1) testStatement
        else object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                lateinit var caughtThrowable: Throwable

                for (i in 1..retryCount) {
                    Log.i(TAG, "${description.displayName}: run #${i}")
                    try {
                        testStatement.evaluate()
                        return
                    } catch (t: Throwable) {
                        caughtThrowable = t
                        Log.e(TAG, "${description.displayName}: run #${i} failed")
                    }
                }

                Log.e(TAG, "${description.displayName}: giving up after $retryCount failures")
                throw caughtThrowable
            }
        }

    companion object {
        private val TAG = RetryTestRule::class.java.canonicalName!!
        private const val RETRY_COUNT = 3
    }
}
