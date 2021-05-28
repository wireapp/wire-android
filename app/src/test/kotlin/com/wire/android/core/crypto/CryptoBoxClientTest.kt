package com.wire.android.core.crypto

import com.wire.android.AndroidTest
import com.wire.android.core.crypto.data.CryptoBoxClientPropertyStorage
import com.wire.android.core.crypto.mapper.CryptoExceptionMapper
import com.wire.android.core.crypto.mapper.PreKeyMapper
import com.wire.android.core.crypto.model.ClientId
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.crypto.model.PlainMessage
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.crypto.model.UserId
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.IOAccessDenied
import com.wire.android.core.exception.InitializationFailure
import com.wire.android.core.exception.SessionNotFound
import com.wire.android.core.exception.UnknownCryptoFailure
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.cryptobox.CryptoBox
import com.wire.cryptobox.CryptoException
import com.wire.cryptobox.CryptoSession
import com.wire.cryptobox.SessionMessage
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Test

typealias CryptoPreKey = com.wire.cryptobox.PreKey

class CryptoBoxClientTest : AndroidTest() {

    @MockK
    private lateinit var propertyStorage: CryptoBoxClientPropertyStorage

    @MockK
    private lateinit var preKeyMapper: PreKeyMapper

    @MockK
    private lateinit var exceptionMapper: CryptoExceptionMapper

    private lateinit var cryptoBox: CryptoBox

    lateinit var subject: CryptoBoxClient

    private val userId = UserId("abc")

    @Before
    fun setup() {
        mockkStatic(System::class) {
            every { System.loadLibrary(any()) } returns Unit
            cryptoBox = mockk()
        }

        val fakeCryptoBoxProvider = object : CryptoBoxProvider {
            override fun cryptoBoxAtPath(path: String): Either<Failure, CryptoBox> = Either.Right(cryptoBox)
        }
        subject = CryptoBoxClient(context(), propertyStorage, userId, preKeyMapper, exceptionMapper, fakeCryptoBoxProvider)
    }

    @Test
    fun `given pre keys are needed, when calling createInitialPreKeys, then cryptoBox is used to create them`() {
        val key = CryptoPreKey(2, byteArrayOf(0, 1, 2))
        every { cryptoBox.newLastPreKey() } returns key
        every { cryptoBox.newPreKeys(any(), any()) } returns arrayOf(key)

        subject.createInitialPreKeys()

        verify(exactly = 1) { cryptoBox.newLastPreKey() }
        verify(exactly = 1) { cryptoBox.newPreKeys(any(), any()) }
    }

    @Test
    fun `given pre keys are needed, when calling createInitialPreKeys, then the mapper should take the results from crypto box`() {
        val lastCryptoKey = CryptoPreKey(2, byteArrayOf(0, 1, 2))
        val regularCryptoKey = CryptoPreKey(42, byteArrayOf(2))
        every { cryptoBox.newLastPreKey() } returns lastCryptoKey
        every { cryptoBox.newPreKeys(any(), any()) } returns arrayOf(regularCryptoKey)

        subject.createInitialPreKeys()

        verify(exactly = 1) { preKeyMapper.fromCryptoBoxModel(lastCryptoKey) }
        verify(exactly = 1) { preKeyMapper.fromCryptoBoxModel(regularCryptoKey) }
    }

    @Test
    fun `given pre keys are needed, when calling createInitialPreKeys, then the mapped data should be returned`() {
        val lastCryptoKey = CryptoPreKey(2, byteArrayOf(0, 1, 2))
        every { cryptoBox.newLastPreKey() } returns lastCryptoKey

        val lastKey = PreKey(1, "a")
        every { preKeyMapper.fromCryptoBoxModel(lastCryptoKey) } returns lastKey

        val regularCryptoKey = CryptoPreKey(42, byteArrayOf(2))
        every { cryptoBox.newPreKeys(any(), any()) } returns arrayOf(regularCryptoKey)

        val lastRegularKey = PreKey(42, "z")
        every { preKeyMapper.fromCryptoBoxModel(regularCryptoKey) } returns lastRegularKey

        subject.createInitialPreKeys().shouldSucceed {
            it.lastKey shouldBeEqualTo lastKey
            it.createdKeys shouldContainSame listOf(lastRegularKey)
        }
    }

    @Test
    fun `given pre keys are needed, when they're generated, then the property storage should be updated`() {
        val cryptoKey = CryptoPreKey(2, byteArrayOf(0, 1, 2))
        every { cryptoBox.newLastPreKey() } returns cryptoKey
        every { cryptoBox.newPreKeys(any(), any()) } returns arrayOf(cryptoKey)

        val key = PreKey(1, "a")
        every { preKeyMapper.fromCryptoBoxModel(any()) } returns key

        subject.createInitialPreKeys()

        verify { propertyStorage.updateLastPreKeyId(any(), any()) }
    }

    @Test
    fun `given pre keys are generated, when updating the last pre key id, then the last regular key id should be passed`() {
        val lastCryptoKey = CryptoPreKey(2, byteArrayOf(0, 1, 2))
        every { cryptoBox.newLastPreKey() } returns lastCryptoKey

        val lastKey = PreKey(1, "a")
        every { preKeyMapper.fromCryptoBoxModel(lastCryptoKey) } returns lastKey

        val lastRegularCryptoKey = CryptoPreKey(42, byteArrayOf(2))
        every { cryptoBox.newPreKeys(any(), any()) } returns arrayOf(lastRegularCryptoKey)

        val lastRegularKey = PreKey(42, "z")
        every { preKeyMapper.fromCryptoBoxModel(lastRegularCryptoKey) } returns lastRegularKey

        subject.createInitialPreKeys()

        verify { propertyStorage.updateLastPreKeyId(userId, lastRegularKey.id) }
    }

    @Test
    fun `givenPreKeysAreBeingGenerated,whenCryptoBoxThrowsAtLastPreKey,thenTheMapperShouldBeUsed`() {
        val expectedException = CryptoException(CryptoException.Code.INIT_ERROR)
        every { cryptoBox.newLastPreKey() } throws expectedException
        every { cryptoBox.newPreKeys(any(), any()) } returns arrayOf(CryptoPreKey(0, byteArrayOf(2)))

        val expectedFailure = InitializationFailure
        every { exceptionMapper.fromNativeException(any()) } returns expectedFailure

        subject.createInitialPreKeys().shouldFail { it shouldBeEqualTo expectedFailure }

        verify { exceptionMapper.fromNativeException(expectedException) }
    }

    @Test
    fun `given last pre key are being generated, when crypto box throws at pre keys, then the mapper should be used`() {
        val expectedException = CryptoException(CryptoException.Code.INIT_ERROR)
        every { cryptoBox.newLastPreKey() } returns CryptoPreKey(0, byteArrayOf(2))
        every { cryptoBox.newPreKeys(any(), any()) } throws expectedException

        val expectedFailure = InitializationFailure
        every { exceptionMapper.fromNativeException(any()) } returns expectedFailure

        subject.createInitialPreKeys().shouldFail { it shouldBeEqualTo expectedFailure }

        verify { exceptionMapper.fromNativeException(expectedException) }
    }

    @Test
    fun `given a message needs encrypting, when there is an error getting the session, should return mappers result`() {
        val expectedException = CryptoException(CryptoException.Code.SESSION_NOT_FOUND)
        every { cryptoBox.getSession(any()) } throws expectedException

        val expectedFailure = SessionNotFound
        every { exceptionMapper.fromNativeException(any()) } returns expectedFailure

        subject.encryptMessage(
            CryptoSessionId(UserId("a"), ClientId("b")),
            PlainMessage(byteArrayOf())
        ) { _ -> Either.Right(Unit) }.shouldFail { it shouldBeEqualTo expectedFailure }

        verify { exceptionMapper.fromNativeException(expectedException) }
    }

    @Test
    fun `given a message was encrypted, when the handler fails, the failure should be propagated`() {
        val session = mockk<CryptoSession>()
        every { cryptoBox.getSession(any()) } returns session
        every { session.encrypt(any()) } returns byteArrayOf()

        val handlerResult = Either.Left(IOAccessDenied)
        subject.encryptMessage(
            CryptoSessionId(UserId("a"), ClientId("b")),
            PlainMessage(byteArrayOf())
        ) { _ -> handlerResult }.shouldFail { it shouldBeEqualTo handlerResult.a }
    }

    @Test
    fun `given a message was encrypted, when the handler fails, the session should not be saved`() {
        val session = mockk<CryptoSession>()
        every { cryptoBox.getSession(any()) } returns session
        every { session.encrypt(any()) } returns byteArrayOf()

        subject.encryptMessage(
            CryptoSessionId(UserId("a"), ClientId("b")),
            PlainMessage(byteArrayOf())
        ) { _ -> Either.Left(IOAccessDenied) }

        verify(exactly = 0) { session.save() }
    }

    @Test
    fun `given a message was encrypted, when the handler succeeds, the session should be saved`() {
        val session = mockk<CryptoSession>()
        every { cryptoBox.getSession(any()) } returns session
        every { session.encrypt(any()) } returns byteArrayOf()
        every { session.save() } returns Unit

        subject.encryptMessage(
            CryptoSessionId(UserId("a"), ClientId("b")),
            PlainMessage(byteArrayOf())
        ) { _ -> Either.Right(Unit) }

        verify(exactly = 1) { session.save() }
    }

    @Test
    fun `given a message needs decrypting, when the session is not found, should attempt to create session based on message`() {
        val notFoundException = CryptoException(CryptoException.Code.SESSION_NOT_FOUND)
        val sessionMessage: SessionMessage = mockk()
        val session: CryptoSession = mockk()
        every { session.save() } returns Unit
        every { sessionMessage.message } returns byteArrayOf()
        every { sessionMessage.session } returns session
        every { cryptoBox.getSession(any()) } throws notFoundException
        every { cryptoBox.initSessionFromMessage(any(), any()) } returns sessionMessage
        every { exceptionMapper.fromNativeException(notFoundException) } returns SessionNotFound

        val sessionId = CryptoSessionId(UserId("a"), ClientId("b"))

        subject.decryptMessage(sessionId, EncryptedMessage(byteArrayOf())) { _ ->
            Either.Right(Unit)
        }.shouldSucceed {}

        verify(exactly = 1) { cryptoBox.initSessionFromMessage(any(), any()) }
    }

    @Test
    fun `given a message needs decrypting, when there is an error getting the session, should return mappers result`() {
        val expectedException = CryptoException(CryptoException.Code.INIT_ERROR)
        every { cryptoBox.getSession(any()) } throws expectedException

        val expectedFailure = UnknownCryptoFailure(expectedException)
        every { exceptionMapper.fromNativeException(any()) } returns expectedFailure

        subject.decryptMessage(
            CryptoSessionId(UserId("a"), ClientId("b")),
            EncryptedMessage(byteArrayOf())
        ) { _ -> Either.Right(Unit) }.shouldFail { it shouldBeEqualTo expectedFailure }

        verify(exactly = 0) { cryptoBox.initSessionFromMessage(any(), any()) }
        verify { exceptionMapper.fromNativeException(expectedException) }
    }

    @Test
    fun `given a message was decrypted, when the handler fails, the failure should be propagated`() {
        val session = mockk<CryptoSession>()
        every { cryptoBox.getSession(any()) } returns session
        every { session.decrypt(any()) } returns byteArrayOf()

        val handlerResult = Either.Left(IOAccessDenied)
        subject.decryptMessage(
            CryptoSessionId(UserId("a"), ClientId("b")),
            EncryptedMessage(byteArrayOf())
        ) { _ -> handlerResult }.shouldFail { it shouldBeEqualTo handlerResult.a }
    }

    @Test
    fun `given a message was decrypted, when the handler fails, the session should not be saved`() {
        val session = mockk<CryptoSession>()
        every { cryptoBox.getSession(any()) } returns session
        every { session.decrypt(any()) } returns byteArrayOf()

        subject.decryptMessage(
            CryptoSessionId(UserId("a"), ClientId("b")),
            EncryptedMessage(byteArrayOf())
        ) { _ -> Either.Left(IOAccessDenied) }

        verify(exactly = 0) { session.save() }
    }

    @Test
    fun `given a message was decrypted, when the handler succeeds, the session should be saved`() {
        val session = mockk<CryptoSession>()
        every { cryptoBox.getSession(any()) } returns session
        every { session.decrypt(any()) } returns byteArrayOf()
        every { session.save() } returns Unit

        subject.decryptMessage(
            CryptoSessionId(UserId("a"), ClientId("b")),
            EncryptedMessage(byteArrayOf())
        ) { _ -> Either.Right(Unit) }

        verify(exactly = 1) { session.save() }
    }

}
