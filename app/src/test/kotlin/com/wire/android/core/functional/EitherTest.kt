package com.wire.android.core.functional

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test

class EitherTest : UnitTest() {

    private lateinit var either: Either<Int, String>

    @Test
    fun `given fold is called, when either is Right, applies fnR and returns its result`() {
        either = Either.Right("Right")

        val result = either.fold({ fail<Int>("Shouldn't be executed") }) { 5 }

        assertThat(result).isEqualTo(5)
    }

    @Test
    fun `given fold is called, when either is Left, applies fnL and returns its result`() {
        either = Either.Left(12)

        val result = either.fold({ "Fold Result" }) { fail<String>("Shouldn't be executed") }

        assertThat(result).isEqualTo("Fold Result")
    }

    @Test
    fun `given flatMap is called, when either is Right, applies function and returns new Either`() {
        either = Either.Right("Right")

        val result = either.flatMap {
            assertThat(it).isEqualTo("Right")
            Either.Left(ServerError)
        }

        assertThat(result).isEqualTo(Either.Left(ServerError))
    }

    @Test
    fun `given flatMap is called, when either is Left, doesn't invoke function and returns original Either`() {
        either = Either.Left(12)

        val result = either.flatMap {
            fail<Either<*, *>>("Shouldn't be executed")
        }

        assertThat(result).isEqualTo(either)
    }

    @Test
    fun `given onFailure is called, when either is Right, doesn't invoke function and returns original Either`() {
        either = Either.Right("Right")

        val result = either.onFailure {
            fail("Shouldn't be executed")
        }

        assertThat(result).isEqualTo(either)
    }

    @Test
    fun `given onFailure is called, when either is Left, invokes function with left value and returns original Either`() {
        either = Either.Left(12)

        var methodCalled = false
        val result = either.onFailure {
            assertThat(it).isEqualTo(12)
            methodCalled = true
        }

        assertThat(result).isEqualTo(either)
        assertThat(methodCalled).isTrue()
    }

    @Test
    fun `given onSuccess is called, when either is Right, invokes function with right value and returns original Either`() {
        either = Either.Right("Right")

        var methodCalled = false
        val result = either.onSuccess {
            assertThat(it).isEqualTo("Right")
            methodCalled = true
        }

        assertThat(result).isEqualTo(either)
        assertThat(methodCalled).isTrue()
    }

    @Test
    fun `given onSuccess is called, when either is Left, doesn't invoke function and returns original Either`() {
        either = Either.Left(12)

        val result = either.onSuccess {
            fail("Shouldn't be executed")
        }

        assertThat(result).isEqualTo(either)
    }

    @Test
    fun `given map is called, when either is Right, invokes function with right value and returns a new Either`() {
        either = Either.Right("Right")

        val result = either.map {
            assertThat(it).isEqualTo("Right")
            "Result"
        }

        assertThat(result).isEqualTo(Either.Right("Result"))
    }

    @Test
    fun `given map is called, when either is Left, doesn't invoke function and returns original Either`() {
        either = Either.Left(12)

        val result = either.map {
            fail<Unit>("Shouldn't be executed")
        }

        assertThat(result).isEqualTo(either)
    }
}
