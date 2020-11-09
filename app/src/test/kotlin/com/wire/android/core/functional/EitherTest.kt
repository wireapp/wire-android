package com.wire.android.core.functional

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class EitherTest : UnitTest() {

    private lateinit var either: Either<Int, String>

    @Test
    fun `given fold is called, when either is Right, applies fnR and returns its result`() {
        either = Either.Right("Success")
        val result = either.fold({ fail("Shouldn't be executed") }) { 5 }

        result shouldBe 5
    }

    @Test
    fun `given fold is called, when either is Left, applies fnL and returns its result`() {
        either = Either.Left(12)

        val foldResult = "Fold Result"
        val result = either.fold({ foldResult }) { fail("Shouldn't be executed") }

        result shouldBe foldResult
    }

    @Test
    fun `given flatMap is called, when either is Right, applies function and returns new Either`() {
        either = Either.Right("Success")

        val result = either.flatMap {
            it shouldBe "Success"
            Either.Left(ServerError)
        }

        result shouldBeEqualTo Either.Left(ServerError)
        result.isLeft shouldBe true
    }

    @Test
    fun `given flatMap is called, when either is Left, doesn't invoke function and returns original Either`() {
        either = Either.Left(12)

        val result: Either<Int, Int> = either.flatMap {
            fail("Shouldn't be executed")
        }

        result.isLeft shouldBe true
        result shouldBeEqualTo either
    }

    @Test
    fun `given onFailure is called, when either is Right, doesn't invoke function and returns original Either`() {
        val success = "Success"
        either = Either.Right(success)

        val result = either.onFailure { fail("Shouldn't be executed") }

        result shouldBe either
        either.getOrElse("Failure") shouldBe success
    }

    @Test
    fun `given onFailure is called, when either is Left, invokes function with left value and returns original Either`() {
        either = Either.Left(12)

        var methodCalled = false
        val result = either.onFailure {
            it shouldBe 12
            methodCalled = true
        }

        result shouldBe either
        methodCalled shouldBe true
    }

    @Test
    fun `given onSuccess is called, when either is Right, invokes function with right value and returns original Either`() {
        val success = "Success"
        either = Either.Right(success)

        var methodCalled = false
        val result = either.onSuccess {
            it shouldBeEqualTo success
            methodCalled = true
        }

        result shouldBe either
        methodCalled shouldBe true
    }

    @Test
    fun `given onSuccess is called, when either is Left, doesn't invoke function and returns original Either`() {
        either = Either.Left(12)

        val result = either.onSuccess {
            fail("Shouldn't be executed")
        }

        result shouldBe either
    }

    @Test
    fun `given map is called, when either is Right, invokes function with right value and returns a new Either`() {
        val success = "Success"
        val resultValue = "Result"
        either = Either.Right(success)

        val result = either.map {
            it shouldBe success
            resultValue
        }

        result shouldBeEqualTo Either.Right(resultValue)
    }

    @Test
    fun `given map is called, when either is Left, doesn't invoke function and returns original Either`() {
        either = Either.Left(12)

        val result = either.map {
            fail("Shouldn't be executed")
        }

        result.isLeft shouldBe true
        result shouldBeEqualTo either
    }
}
