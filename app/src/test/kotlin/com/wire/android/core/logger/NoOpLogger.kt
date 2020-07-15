package com.wire.android.core.logger

import com.wire.android.any
import org.mockito.Mockito.lenient
import org.mockito.Mockito.mock

/**
 * Creates a mock [Logger] which does not log anything. The real implementation uses [android.util.Log], which throws exceptions in unit
 * test environment due to lack of android dependencies. noOpLogger() can be used for convenience in unit tests to skip any logging calls.
 *
 * ```
 * class MyObjectWithLogger(val logger: Logger) {
 *
 *      fun someMethod() {
 *          logger.i("TAG", "message")
 *          ...
 *      }
 * }
 *
 *
 * class MyObjectWithLoggerTest {
 *
 *      @Test
 *      fun testMethod() {
 *          val logger = noOpLogger()
 *          val myObject = MyObjectWithLogger(logger)
 *
 *          // log calls won't throw exceptions now
 *          myObject.someMethod()
 *
 *          // you can also make assertions
 *          verify(logger).e(TAG, message)
 *      }
 * }
 * ```
 *
 * @return mock logger
 */
fun noOpLogger(): Logger = mock(Logger::class.java).also {
    lenient().doNothing().`when`(it).e(any(), any())
    lenient().doNothing().`when`(it).w(any(), any())
    lenient().doNothing().`when`(it).d(any(), any())
    lenient().doNothing().`when`(it).i(any(), any())
    lenient().doNothing().`when`(it).v(any(), any())
}
