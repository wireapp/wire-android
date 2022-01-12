package com.wire.android

import io.mockk.MockKAnnotations
import org.junit.rules.TestRule

object InjectMockKsRule {
    fun create(testClass: Any) = TestRule { statement, _ ->
        MockKAnnotations.init(testClass, relaxUnitFun = true, relaxed = true)
        statement
    }
}
