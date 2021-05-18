package com.wire.android.core.crypto

import com.wire.android.InstrumentationTest
import com.wire.android.core.crypto.data.CryptoBoxClientPropertyStorage
import com.wire.android.core.crypto.mapper.CryptoExceptionMapper
import com.wire.android.core.crypto.mapper.PreKeyMapper
import com.wire.android.core.crypto.model.ClientId
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.crypto.model.PlainMessage
import com.wire.android.core.crypto.model.UserId
import com.wire.android.core.exception.MessageAlreadyDecrypted
import com.wire.android.core.exception.SessionNotFound
import com.wire.android.core.functional.Either
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test

class CryptoBoxClientIntegrationTest : InstrumentationTest() {

    private val alice = UserId("Alice")
    private val aliceClientId = ClientId("clientA")
    private val aliceSessionId = CryptoSessionId(alice, aliceClientId)

    private val bob = UserId("Bob")

    private lateinit var aliceClient: CryptoBoxClient
    private lateinit var bobClient: CryptoBoxClient

    @Before
    fun setup() {
        aliceClient =
            CryptoBoxClient(appContext, CryptoBoxClientPropertyStorage(appContext), alice, PreKeyMapper(), CryptoExceptionMapper())
        bobClient =
            CryptoBoxClient(appContext, CryptoBoxClientPropertyStorage(appContext), bob, PreKeyMapper(), CryptoExceptionMapper())
    }

    @After
    fun tearDown() {
        aliceClient.delete()
        bobClient.delete()
    }

    @Test
    fun givenBobWantsToTalkToAlice_whenSendingTheFirstMessageAndTheSessionIsAsserted_itShouldBeEncryptedSuccessfully() {
        val aliceKey = (aliceClient.createInitialPreKeys() as Either.Right).b.lastKey

        bobClient.assertSession(aliceSessionId, aliceKey)
        val result = bobClient.encryptMessage(aliceSessionId, PlainMessage("Hello".toByteArray())) { Either.Right(Unit) }
        result.isRight shouldBe true
    }

    @Test
    fun givenBobWantsToTalkToAlice_whenSendingTheFirstMessageWithoutHavingTheSessionAsserted_itShouldFailWithSessionNotFound() {
        val result = bobClient.encryptMessage(aliceSessionId, PlainMessage("Hello".toByteArray())) { Either.Right(Unit) }
        result.isLeft shouldBe true
        (result as Either.Left).a shouldBeInstanceOf SessionNotFound::class
    }

    @Test
    fun givenBobSendsTheFirstMessage_whenAliceReceivesIt_itShouldBeDecryptedSuccessfully() {
        val aliceKey = (aliceClient.createInitialPreKeys() as Either.Right).b.lastKey

        bobClient.assertSession(aliceSessionId, aliceKey)

        val plainMessage = PlainMessage("Hello".toByteArray())
        val result = bobClient.encryptMessage(aliceSessionId, plainMessage) { encryptedMessage ->

            val decryptedResult =
                aliceClient.decryptMessage(CryptoSessionId(bob, ClientId("doesntmatter")), encryptedMessage) { decryptedMessage ->
                    decryptedMessage.data shouldBeEqualTo plainMessage.data
                    Either.Right(Unit)
                }
            decryptedResult.isRight shouldBe true
            Either.Right(Unit)
        }
        result.isRight shouldBe true
    }

    @Test
    fun givenAliceForgetsTheDecryptedMessageContent_whenAttemptingToDecryptItAgain_itShouldFailWithDuplicatedMessage() {
        val aliceKey = (aliceClient.createInitialPreKeys() as Either.Right).b.lastKey

        bobClient.assertSession(aliceSessionId, aliceKey)

        var firstMessage: EncryptedMessage? = null
        bobClient.encryptMessage(aliceSessionId, PlainMessage("Opa, tudo bom guri?".toByteArray())) { encryptedMessage ->
            firstMessage = encryptedMessage
            Either.Right(Unit)
        }

        val bobSessionID = CryptoSessionId(bob, ClientId("clientB"))

        val firstDecryptionResult = aliceClient.decryptMessage(bobSessionID, firstMessage!!) { Either.Right(Unit) }
        firstDecryptionResult.isRight shouldBe true

        val repeatedDecryptionResult = aliceClient.decryptMessage(bobSessionID, firstMessage!!) { decryptedMessage ->
            Either.Right(Unit)
        }
        repeatedDecryptionResult.isLeft shouldBe true
        (repeatedDecryptionResult as Either.Left).a shouldBeInstanceOf MessageAlreadyDecrypted::class
    }

}
