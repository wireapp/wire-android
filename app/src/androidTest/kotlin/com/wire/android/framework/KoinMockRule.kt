package com.wire.android.framework

import org.koin.test.KoinTest
import org.koin.test.mock.MockProviderRule
import org.mockito.Mockito

fun KoinTest.koinMockRule() = MockProviderRule.create { clazz -> Mockito.mock(clazz.java) }
