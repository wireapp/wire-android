package com.wire.android.core.usecase

import com.wire.android.UnitTest
import com.wire.android.core.functional.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class DefaultUseCaseExecutorTest : UnitTest() {

    private lateinit var executor: DefaultUseCaseExecutor

    @Mock
    private lateinit var useCase: UseCase<String, Int>

    @Before
    fun setUp() {
        executor = DefaultUseCaseExecutor()
    }

    @Test
    fun `given a scope and a use case, when invoke is called on use case, then applies onResult to returned value`() {
        runBlocking {
            val param = 3
            val result = Either.Right("3")
            `when`(useCase.run(param)).thenReturn(result)

            with(executor) {
                useCase.invoke(this@runBlocking, param, Dispatchers.IO) {
                    assertThat(it).isEqualTo(result)
                }
            }
        }
    }
}
