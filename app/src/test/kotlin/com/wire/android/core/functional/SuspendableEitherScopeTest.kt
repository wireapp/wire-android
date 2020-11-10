package com.wire.android.core.functional

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class SuspendableEitherScopeTest : UnitTest() {

    private lateinit var either: Either<Int, String>

    private lateinit var suspendableEitherScope: SuspendableEitherScope

    @Before
    fun setUp() {
        suspendableEitherScope = SuspendableEitherScope()
    }

    @Test
    fun `given coFold is called on an Either, when either is Right, applies fnR and returns its result`() {
        either = Either.Right("Success")

        val result = runBlocking {
            with(suspendableEitherScope) {
                either.coFold({ fail("Shouldn't be executed") }) { 5 }
            }
        }

        result shouldBeEqualTo 5
    }

    @Test
    fun `given coFold is called on an Either, when either is Left, applies fnL and returns its result`() {
        either = Either.Left(12)
        val foldResult = "Fold Result"

        val result = runBlocking {
            with(suspendableEitherScope) {
                either.coFold({ foldResult }) { fail("Shouldn't be executed") }
            }
        }

        result shouldBe foldResult
    }

    @Test
    fun `given flatMap is called on an Either, when either is Right, applies function and returns new Either`() {
        either = Either.Right("Success")

        val result = runBlocking {
            with(suspendableEitherScope) {

                either.flatMap {
                    it shouldBe "Success"
                    Either.Left(ServerError)
                }
            }
        }

        result shouldBeEqualTo Either.Left(ServerError)
        result.isLeft shouldBe true
    }

    @Test
    fun `given flatMap is called on an Either, when either is Left, doesn't invoke function and returns original Either`() {
        either = Either.Left(12)

        val result: Either<Int, Int> = runBlocking {
            with(suspendableEitherScope) {
                either.flatMap { fail("Shouldn't be executed") }
            }
        }
        result.isLeft shouldBe true
        result shouldBeEqualTo either
    }

    @Test
    fun `given onFailure is called on an Either, when either is Right, doesn't invoke function and returns original Either`() {
        val success = "Success"
        either = Either.Right(success)

        val result = runBlocking {
            with(suspendableEitherScope) {
                either.onFailure { fail("Shouldn't be executed") }
            }
        }

        result shouldBe either
        either.getOrElse("Failure") shouldBe success
    }

    @Test
    fun `given onFailure is called on an Either, when either is Left, invokes function with left value and returns original Either`() {
        either = Either.Left(12)
        var methodCalled = false

        val result = runBlocking {
            with(suspendableEitherScope) {

                either.onFailure {
                    it shouldBeEqualTo 12
                    methodCalled = true
                }
            }
        }

        result shouldBe either
        methodCalled shouldBe true
    }

    @Test
    fun `given onSuccess is called on an Either, when either is Right, invokes function with right value and returns original Either`() {
        val success = "Success"
        either = Either.Right(success)
        var methodCalled = false

        val result = runBlocking {
            with(suspendableEitherScope) {

                either.onSuccess {
                    it shouldBeEqualTo success
                    methodCalled = true
                }
            }
        }

        result shouldBe either
        methodCalled shouldBe true
    }

    @Test
    fun `given onSuccess is called on an Either, when either is Left, doesn't invoke function and returns original Either`() {
        either = Either.Left(12)

        val result = runBlocking {
            with(suspendableEitherScope) {
                either.onSuccess { fail("Shouldn't be executed") }
            }
        }

        result shouldBe either
    }

    @Test
    fun `given map is called on an Either, when either is Right, invokes function with right value and returns a new Either`() {
        val success = "Success"
        val resultValue = "Result"
        either = Either.Right(success)

        val result = runBlocking {
            with(suspendableEitherScope) {

                 either.map {
                    it shouldBe success
                    resultValue
                }
            }
        }

        result shouldBeEqualTo Either.Right(resultValue)
    }

    @Test
    fun `given map is called on an Either, when either is Left, doesn't invoke function and returns original Either`() {
        either = Either.Left(12)

        val result = runBlocking {
            with(suspendableEitherScope) {
                 either.map { fail("Shouldn't be executed") }
            }
        }

        result.isLeft shouldBe true
        result shouldBeEqualTo either
    }

    @Test
    fun `given a block, when suspending is called, then creates a new SuspendableEitherScope and executes block on it`() {
        val block: SuspendableEitherScope.() -> Unit = mockk(relaxed = true)

        runBlocking {
            suspending {
                this shouldBeInstanceOf SuspendableEitherScope::class
                block()
            }
        }

        verify(exactly = 1) { block.invoke(any()) }
    }
}
