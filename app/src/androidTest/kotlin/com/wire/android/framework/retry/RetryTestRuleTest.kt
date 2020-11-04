package com.wire.android.framework.retry

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RetryTestRuleTest {

    private lateinit var description: Description

    @Before
    fun setUp() {
        description = Description.createTestDescription(RetryTestRuleTest::class.java, "Dummy description for test")
    }

    @Test
    fun apply_retryCountIsNotGreaterThanOne_returnsOriginalStatement() {
        val originalStatement = StatementWithCount {}
        val ruleWithZeroRetries = RetryTestRule(0)

        val retryStatement = ruleWithZeroRetries.apply(originalStatement, description)

        retryStatement shouldBeEqualTo originalStatement
    }

    @Test
    fun apply_retryCountIsGreaterThanOne_statementFails_retriesUpToRetryCountTimes() {
        val retryCount = 5
        val originalStatement = StatementWithCount {
            if (it < retryCount) throw AssertionError("Test failed at iteration #$it")
        }

        val rule = RetryTestRule(retryCount)

        val retryStatement = rule.apply(originalStatement, description)
        retryStatement.evaluate()

        originalStatement.evaluationCount shouldBeEqualTo retryCount
        retryStatement shouldNotBeEqualTo originalStatement
    }

    @Test
    fun apply_retryCountIsGreaterThanOne_statementSucceedsBeforeRetryCountIterations_returnsSuccessImmediately() {
        val retryCount = 5
        val originalStatement = StatementWithCount {
            if (it < 3) throw AssertionError("Test failed at iteration #$it")
        }

        val rule = RetryTestRule(retryCount)

        val retryStatement = rule.apply(originalStatement, description)
        retryStatement.evaluate()

        originalStatement.evaluationCount shouldBeEqualTo 3
        retryStatement shouldNotBeEqualTo originalStatement
    }

    @Test
    fun apply_retryCountIsGreaterThanOne_statementFailsAtEachRetry_retriesAndReturnsFailureEventually() {
        val retryCount = 5
        val originalStatement = StatementWithCount {
            throw AssertionError("Test failed at iteration #$it")
        }

        val rule = RetryTestRule(retryCount)
        val retryStatement = rule.apply(originalStatement, description)

        try {
            retryStatement.evaluate()
        } catch (ex: AssertionError) {
            originalStatement.evaluationCount shouldBeEqualTo retryCount
            retryStatement shouldNotBeEqualTo originalStatement
            return
        }

        fail("Should have thrown AssertionError")
    }

    private inner class StatementWithCount(val evaluateAction: (Int) -> Unit) : Statement() {
        var evaluationCount = 0

        override fun evaluate() {
            evaluationCount++
            evaluateAction(evaluationCount)
        }
    }
}
