package com.wire.android.core.crypto

import com.wire.android.InstrumentationTest
import com.wire.android.core.crypto.data.CryptoBoxClientPropertyStorage
import com.wire.android.core.crypto.mapper.CryptoExceptionMapper
import com.wire.android.core.crypto.mapper.CryptoPreKeyMapper
import com.wire.android.core.crypto.model.CryptoClientId
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.crypto.model.PlainMessage
import com.wire.android.core.crypto.model.UserId
import com.wire.android.core.exception.MessageAlreadyDecrypted
import com.wire.android.core.exception.SessionNotFound
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test

class CryptoBoxClientIntegrationTest : InstrumentationTest() {

    private val alice = UserId("Alice")
    private val aliceClientId = CryptoClientId("clientA")
    private val aliceSessionId = CryptoSessionId(alice, aliceClientId)

    private val bob = UserId("Bob")

    private lateinit var aliceClient: CryptoBoxClient
    private lateinit var bobClient: CryptoBoxClient

    @Before
    fun setup() {
        aliceClient =
            CryptoBoxClient(
                appContext,
                CryptoBoxClientPropertyStorage(appContext),
                alice,
                CryptoPreKeyMapper(),
                CryptoExceptionMapper(),
                DefaultCryptoBoxProvider
            )
        bobClient =
            CryptoBoxClient(
                appContext,
                CryptoBoxClientPropertyStorage(appContext),
                bob,
                CryptoPreKeyMapper(),
                CryptoExceptionMapper(),
                DefaultCryptoBoxProvider
            )
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
        bobClient.encryptMessage(aliceSessionId, PlainMessage("Hello".toByteArray())) { Either.Right(Unit) }
            .shouldFail { it shouldBeInstanceOf SessionNotFound::class }
    }

    @Test
    fun givenBobSendsTheFirstMessage_whenAliceReceivesIt_itShouldBeDecryptedSuccessfully() {
        val aliceKey = (aliceClient.createInitialPreKeys() as Either.Right).b.lastKey

        bobClient.assertSession(aliceSessionId, aliceKey)

        val plainMessage = PlainMessage("Hello".toByteArray())
        bobClient.encryptMessage(aliceSessionId, plainMessage) { encryptedMessage ->

            runBlocking {
                aliceClient.decryptMessage(CryptoSessionId(bob, CryptoClientId("doesntmatter")), encryptedMessage) { decryptedMessage ->
                    decryptedMessage.data shouldBeEqualTo plainMessage.data
                    Either.Right(Unit)
                }
            }.shouldSucceed { }

            Either.Right(Unit)
        }.shouldSucceed { }
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

        val bobSessionID = CryptoSessionId(bob, CryptoClientId("clientB"))

        runBlocking {
            aliceClient.decryptMessage(bobSessionID, firstMessage!!) { Either.Right(Unit) }
        }.shouldSucceed { }

        runBlocking {
            aliceClient.decryptMessage(bobSessionID, firstMessage!!) { Either.Right(Unit) }
        }.shouldFail { it shouldBeInstanceOf MessageAlreadyDecrypted::class }
    }
}
