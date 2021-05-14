package com.wire.android.core.crypto

import androidx.test.filters.RequiresDevice
import com.wire.android.InstrumentationTest
import com.wire.android.core.crypto.data.CryptoBoxClientPropertyStorage
import com.wire.android.core.crypto.mapper.CryptoExceptionMapper
import com.wire.android.core.crypto.mapper.PreKeyMapper
import com.wire.android.core.crypto.model.ClientId
import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.crypto.model.PlainMessage
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.UserId
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.onSuccess
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test

@RequiresDevice
class AliceAndBobCryptoBoxClientTest : InstrumentationTest() {

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
        bobClient.encryptMessage(aliceSessionId, PlainMessage("Hello".toByteArray())) { result ->
            result.isRight shouldBe true
            true
        }
    }

    @Test
    fun givenBobWantsToTalkToAlice_whenSendingTheFirstMessageWithoutHavingTheSessionAsserted_itShouldFailWithSessionNotFound() {
        bobClient.encryptMessage(aliceSessionId, PlainMessage("Hello".toByteArray())) { result ->
            result.isLeft shouldBe true
            (result as Either.Left).a shouldBeInstanceOf SessionNotFound::class
            true
        }
    }

    @Test
    fun givenBobSendsTheFirstMessage_whenAliceReceivesIt_itShouldBeDecryptedSuccessfully() {
        val aliceKey = (aliceClient.createInitialPreKeys() as Either.Right).b.lastKey

        bobClient.assertSession(aliceSessionId, aliceKey)

        val plainMessage = PlainMessage("Hello".toByteArray())
        bobClient.encryptMessage(aliceSessionId, plainMessage) { result ->
            result.isRight shouldBe true
            result.onSuccess { encryptedMessage: EncryptedMessage ->

                aliceClient.decryptMessage(CryptoSessionId(bob, ClientId("doesntmatter")), encryptedMessage) { decryptedResult ->
                    decryptedResult.isRight shouldBe true
                    decryptedResult.onSuccess { decryptedMessage ->
                        decryptedMessage.data shouldBeEqualTo plainMessage.data
                    }
                    true
                }
            }
            true
        }
    }

    @Test
    fun givenAliceForgetsTheDecryptedMessageContent_whenAttemptingToDecryptItAgain_itShouldFailWithDuplicatedMessage() {
        val aliceKey = (aliceClient.createInitialPreKeys() as Either.Right).b.lastKey

        bobClient.assertSession(aliceSessionId, aliceKey)

        var firstMessage: EncryptedMessage? = null
        bobClient.encryptMessage(aliceSessionId, PlainMessage("Opa, tudo bom guri?".toByteArray())) {
            firstMessage = (it as Either.Right).b
            true
        }

        val bobSessionID = CryptoSessionId(bob, ClientId("clientB"))

        aliceClient.decryptMessage(bobSessionID, firstMessage!!) { decryptedResult ->
            decryptedResult.isRight shouldBe true
            true
        }

        aliceClient.decryptMessage(bobSessionID, firstMessage!!) { decryptedResult ->
            decryptedResult.isLeft shouldBe true
            (decryptedResult as Either.Left).a shouldBeInstanceOf DuplicatedMessage::class
            true
        }
    }

}
