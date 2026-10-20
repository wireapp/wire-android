/* Ignored measurement fixture, derived from the verified WireKaliumConsumerIntegrationTest.
 * Parent stages exactly one API variant; this file has not been Gradle-compiled by its author.
 */
package com.wire.android.di.metro

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountTokens
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.di.KaliumConsumer
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageByIdUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import com.wire.kalium.persistence.dao.SupportedProtocolEntity
import com.wire.kalium.persistence.dao.UserAvailabilityStatusEntity
import com.wire.kalium.persistence.dao.UserEntity
import com.wire.kalium.persistence.dao.UserIDEntity
import com.wire.kalium.persistence.dao.UserTypeEntity
import com.wire.kalium.persistence.dao.conversation.ConversationEntity
import com.wire.kalium.persistence.dao.message.MessageEntity
import com.wire.kalium.persistence.dao.message.MessageEntityContent
import com.wire.kalium.persistence.daokaliumdb.ServerConfigurationDAO
import com.wire.kalium.persistence.db.GlobalDatabaseBuilder
import com.wire.kalium.persistence.db.PlatformDatabaseData
import com.wire.kalium.persistence.db.globalDatabaseProvider
import com.wire.kalium.persistence.db.userDatabaseBuilder
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelMultibindings
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.lang.reflect.InvocationTargetException
import java.util.UUID
import java.util.function.Supplier
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.SQLiteMode


@RunWith(RobolectricTestRunner::class)
@Config(
    application = Application::class,
    sdk = [34],
    instrumentedPackages = ["com.waz.audioeffect"],
    shadows = [WireKaliumMeasurementAudioEffectShadow::class],
)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class WireKaliumMeasurementTest {
    @Test
    fun measureSessionAndActualViewModels() = runBlocking {
        val warmups = System.getProperty("metro.measure.warmups", "3").toInt()
        val repetitions = System.getProperty("metro.measure.repetitions", "7").toInt()
        val warmVms = System.getProperty("metro.measure.warmVms", "10").toInt()
        val observationMs = System.getProperty("metro.measure.observationMs", "250").toLong()
        require(warmups >= 0 && repetitions > 0 && warmVms > 0 && observationMs >= 0)
        val startupOnly = System.getProperty("metro.measure.scenario", "vm") == "startup-only"
        Meter.provenance()
        mockkObject(WorkManager.Companion)
        try {
            every { WorkManager.getInstance(any<Context>()) } returns mockk(relaxed = true)
            repeat(warmups + repetitions) { iteration ->
                val warming = iteration < warmups
                val owners = mutableListOf<MeasurementOwner>()
                var fixture: MeasurementFixture? = null
                var factory: MeasurementFactory? = null
                var firstConsumer: KaliumConsumer? = null
                try {
                    withTimeout(90_000) {
                        // Seed/global CoreLogic/account registration excluded from SESSION measurement.
                        val sdk = MeasurementFixture.create().also { fixture = it }
                        val session = Meter.measure("cold_session_get_or_create", iteration, warming) {
                            sdk.logic.getSessionScope(sdk.userId)
                        }
                        assertSame(session, sdk.logic.getSessionScope(sdk.userId))
                        if (startupOnly) {
                            Meter.measure("mandatory_startup_observation_no_vm", iteration, warming) {
                                Thread.sleep(observationMs)
                            }
                        } else {
                            val owner = MeasurementOwner().also(owners::add)
                            val first = Meter.measure("first_vm_new_store", iteration, warming) {
                                MeasurementFactory(session).also { factory = it }.let {
                                    ViewModelProvider(owner, it)["screen", MeasurementViewModel::class.java]
                                }
                            }
                            val dispatcher = requireNotNull(factory)
                            // Transfer only after the probe window closes. The retained first VM owns this consumer.
                            firstConsumer = requireNotNull(dispatcher.takeCreatedConsumer())
                            assertSame(first.consumer, firstConsumer)
                            val retained = Meter.measure("retained_vm_lookup", iteration, warming) {
                                ViewModelProvider(owner, dispatcher)["screen", MeasurementViewModel::class.java]
                            }
                            assertSame(first, retained)
                            dispatcher.assertCreated(1)
                            // Actual persisted reads are validated OUTSIDE VM creation timing.
                            validateReads(first, sdk)
                            Meter.measure("mandatory_startup_overlap_observation", iteration, warming) {
                                // Fixed observation interval, NOT a startup-complete barrier or pure startup cost.
                                Thread.sleep(observationMs)
                            }
                            repeat(warmVms) {
                                val nextOwner = MeasurementOwner().also(owners::add)
                                try {
                                    val next = Meter.measure("warm_vm_new_store", iteration, warming) {
                                        ViewModelProvider(nextOwner, dispatcher)["screen", MeasurementViewModel::class.java]
                                    }
                                    check(next !== first)
                                    validateReads(next, sdk)
                                } finally {
                                    // Also drain failed construction/read paths, AFTER Meter has ended its window.
                                    withContext(NonCancellable) {
                                        withTimeout(30_000) {
                                            try { nextOwner.viewModelStore.clear() }
                                            finally { dispatcher.drainCreatedConsumer() }
                                        }
                                    }
                                }
                            }
                            dispatcher.assertCreated(1 + warmVms)
                            assertSame(session, sdk.logic.getSessionScope(sdk.userId))
                        }
                    }
                } finally {
                    withContext(NonCancellable) {
                        try {
                            withTimeout(30_000) {
                                try { owners.forEach { it.viewModelStore.clear() } }
                                finally {
                                    try { firstConsumer?.closeAndJoin() }
                                    finally { factory?.drainCreatedConsumer() }
                                }
                            }
                        } finally {
                            firstConsumer = null
                            withTimeout(30_000) { fixture?.close() }
                        }
                    }
                }
            }
        } finally {
            try { Meter.finish() } finally { unmockkObject(WorkManager.Companion) }
        }
    }

    private suspend fun validateReads(vm: MeasurementViewModel, sdk: MeasurementFixture) {
        val message = (vm.message(sdk.conversationId, MESSAGE_ID)
            .first { it is ObserveMessageByIdUseCase.Result.Success } as ObserveMessageByIdUseCase.Result.Success).message
        assertEquals(MESSAGE_ID, message.id)
        assertEquals(MESSAGE_TEXT, (message.content as MessageContent.Text).value)
        val details = (vm.details(sdk.conversationId)
            .first { it is ObserveConversationDetailsUseCase.Result.Success } as ObserveConversationDetailsUseCase.Result.Success)
            .conversationDetails.conversation
        assertEquals(sdk.conversationId, details.id)
        assertEquals(CONVERSATION_NAME, details.name)
        val lookup = (vm.getMessage(sdk.conversationId, MESSAGE_ID) as GetMessageByIdUseCase.Result.Success).message
        check(lookup is Message.Regular)
        assertEquals(MESSAGE_TEXT, (lookup.content as MessageContent.Text).value)
    }

    private class MeasurementOwner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    private class MeasurementFactory(private val session: UserSessionScope) : WireConsumerMetroViewModelFactory() {
        private var created = 0
        // At most one in-flight creation; taken/drained outside measurement, including helper failure.
        private var createdConsumer: KaliumConsumer? = null
        private val graphFactory = createGraphFactory<MeasurementGraph.Factory>()
        override fun <VM : ViewModel> createInGraph(create: (MetroViewModelFactory) -> VM): VM {
            check(createdConsumer == null) { "Previous measured consumer has not been transferred/drained" }
            return createConsumerViewModel(createConsumer = {
                session.createConsumer().also { createdConsumer = it; created++ }
            }) { consumer ->
                create(graphFactory.create(consumer).factory)
            }
        }
        fun assertCreated(expected: Int) { assertEquals(expected, created) }
        fun takeCreatedConsumer(): KaliumConsumer? = createdConsumer.also { createdConsumer = null }
        suspend fun drainCreatedConsumer() { takeCreatedConsumer()?.closeAndJoin() }
    }

    private class MeasurementFixture(val logic: CoreLogic, val userId: UserId, val conversationId: ConversationId, val configId: String) {
        suspend fun login(replace: Boolean = false) {
            assertEquals(
                AddAuthenticatedUserUseCase.Result.Success(userId),
                logic.getGlobalScope().addAuthenticatedAccount(
                    StoreSessionParam(
                        serverConfigId = configId,
                        ssoId = null,
                        accountTokens = AccountTokens(userId, "access", "refresh", "Bearer", null),
                        proxyCredentials = null,
                        isPersistentWebSocketEnabled = false,
                    ),
                    replace = replace,
                ),
            )
        }

        suspend fun close() {
            try {
                assertEquals(DeleteSessionUseCase.Result.Success, logic.getGlobalScope().deleteSession(userId))
            } finally {
                try {
                    logic.getGlobalScope().coroutineContext.job.cancelAndJoin()
                } finally {
                    // Existing protected getter verified in both emitted Android CoreLogic classfiles.
                    // Teardown only; no new production API. One CoreLogic/global DB owner per cycle.
                    val getter = CoreLogic::class.java.getDeclaredMethod("getGlobalDatabaseBuilder")
                    getter.isAccessible = true
                    (getter.invoke(logic) as GlobalDatabaseBuilder).close()
                }
            }
        }

        companion object {
            suspend fun create(): MeasurementFixture {
                val context = ApplicationProvider.getApplicationContext<Application>()
                val userId = UserId(UUID.randomUUID().toString(), "consumer-integration.invalid")
                val conversationId = ConversationId("conversation", userId.domain)
                val configId = "$SERVER_CONFIG_ID-${userId.value}"
                val platform = PlatformDatabaseData(context)
                // No SDK internals or testing hooks: these are the existing public platform providers.
                val global = globalDatabaseProvider(platform, Dispatchers.IO, passphrase = null)
                try {
                    global.serverConfigurationDAO.insert(
                        ServerConfigurationDAO.InsertData(
                            id = configId,
                            apiBaseUrl = URL,
                            accountBaseUrl = URL,
                            webSocketBaseUrl = "wss://consumer-integration.invalid",
                            blackListUrl = URL,
                            teamsUrl = URL,
                            websiteUrl = URL,
                            title = "Consumer integration",
                            isOnPremises = false,
                            federation = false,
                            domain = userId.domain,
                            commonApiVersion = 8,
                            apiProxyHost = null,
                            apiProxyNeedsAuthentication = null,
                            apiProxyPort = null,
                        ),
                    )
                } finally {
                    global.close()
                }
                seedMessages(platform, userId, conversationId)
                val logic = CoreLogic(
                    userAgent = "wire-consumer-integration-test",
                    appContext = context,
                    rootPath = context.filesDir.absolutePath,
                    kaliumConfigs = KaliumConfigs(
                        enableCalling = false,
                        shouldEncryptData = { false },
                        mockedRequests = emptyList(),
                    ),
                )
                return MeasurementFixture(logic, userId, conversationId, configId).also {
                    try {
                        it.login()
                    } catch (failure: Throwable) {
                        withContext(NonCancellable) { it.close() }
                        throw failure
                    }
                }
            }

            private suspend fun seedMessages(platform: PlatformDatabaseData, userId: UserId, conversationId: ConversationId) {
                val self = UserIDEntity(userId.value, userId.domain)
                val database = userDatabaseBuilder(
                    platformDatabaseData = platform,
                    userId = self,
                    passphrase = null,
                    dispatcher = Dispatchers.IO,
                    enableWAL = true,
                    dbInvalidationControlEnabled = false,
                )
                val date = Instant.parse("2026-10-20T10:00:00Z")
                try {
                    database.userDAO.upsertUser(persistedUser(self))
                    val conversation = ConversationEntity(
                        id = UserIDEntity(conversationId.value, conversationId.domain),
                        name = CONVERSATION_NAME,
                        type = ConversationEntity.Type.GROUP,
                        teamId = null,
                        protocolInfo = ConversationEntity.ProtocolInfo.Proteus,
                        creatorId = userId.value,
                        lastNotificationDate = null,
                        lastModifiedDate = date,
                        lastReadDate = date,
                        access = listOf(ConversationEntity.Access.INVITE),
                        accessRole = listOf(ConversationEntity.AccessRole.NON_TEAM_MEMBER),
                        receiptMode = ConversationEntity.ReceiptMode.DISABLED,
                        messageTimer = null,
                        userMessageTimer = null,
                        archivedInstant = null,
                        mlsVerificationStatus = ConversationEntity.VerificationStatus.NOT_VERIFIED,
                        proteusVerificationStatus = ConversationEntity.VerificationStatus.NOT_VERIFIED,
                        legalHoldStatus = ConversationEntity.LegalHoldStatus.DISABLED,
                        channelAccess = null,
                        channelAddPermission = null,
                        wireCell = null,
                        historySharingRetentionSeconds = 0,
                    )
                    database.conversationDAO.insertConversation(conversation)
                    database.messageDAO.insertOrIgnoreMessage(
                        MessageEntity.Regular(
                            id = MESSAGE_ID,
                            conversationId = conversation.id,
                            date = date,
                            senderUserId = self,
                            senderClientId = "unregistered-sender",
                            senderName = "Integration user",
                            status = MessageEntity.Status.SENT,
                            content = MessageEntityContent.Text(MESSAGE_TEXT),
                            isSelfMessage = true,
                            readCount = 0,
                            editStatus = MessageEntity.EditStatus.NotEdited,
                        ),
                    )
                } finally {
                    database.close()
                }
            }

            private fun persistedUser(self: UserIDEntity): UserEntity = UserEntity(
                id = self, name = "Integration user", handle = null, email = null, phone = null,
                accentId = 0, team = null, previewAssetId = null, completeAssetId = null,
                availabilityStatus = UserAvailabilityStatusEntity.NONE, userType = UserTypeEntity.FEDERATED,
                botService = null, deleted = false, expiresAt = null, defederated = false,
                supportedProtocols = setOf(SupportedProtocolEntity.PROTEUS), activeOneOnOneConversationId = null,
            )
        }
    }

    private companion object {
        const val SERVER_CONFIG_ID = "wire-consumer-measurement"
        const val URL = "https://consumer-integration.invalid"
        const val CONVERSATION_NAME = "Persisted integration conversation"
        const val MESSAGE_ID = "persisted-integration-message"
        const val MESSAGE_TEXT = "A real SQLite message reached the Android ViewModel"
    }
}

internal class MeasurementViewModel @Inject constructor(
    val consumer: KaliumConsumer,
    val details: ObserveConversationDetailsUseCase,
    val message: ObserveMessageByIdUseCase,
    val getMessage: GetMessageByIdUseCase,
) : ViewModel()

@DependencyGraph(bindingContainers = [KaliumConsumerModule::class, MeasurementBindings::class])
internal interface MeasurementGraph : MetroViewModelMultibindings {
    val factory: WireViewModelGraphFactory
    @DependencyGraph.Factory interface Factory {
        fun create(@Provides consumer: KaliumConsumer): MeasurementGraph
    }
}
@BindingContainer internal object MeasurementBindings {
    @Provides @IntoMap @ViewModelKey(MeasurementViewModel::class)
    fun viewModel(vm: MeasurementViewModel): ViewModel = vm
}

/** Calls the bootstrap API reflectively OUTSIDE its timed Supplier boundary. */
private object Meter {
    private val probe = Class.forName("com.wire.metro.measure.Probe", true, null)
    private val measure = probe.getMethod("measure", String::class.java, Integer.TYPE, java.lang.Boolean.TYPE, Supplier::class.java)
    init {
        check(probe.classLoader == null)
        val installed = probe.getMethod("isInstalled").invoke(null) as Boolean
        check(installed == (System.getProperty("metro.measure.pass") == "constructors"))
    }
    @Suppress("UNCHECKED_CAST")
    fun <T> measure(phase: String, iteration: Int, warmup: Boolean, block: () -> T): T = try {
        measure.invoke(null, phase, iteration, warmup, Supplier { block() }) as T
    } catch (failure: InvocationTargetException) { throw failure.targetException }
    fun provenance() { probe.getMethod("provenance").invoke(null) }
    fun finish() { probe.getMethod("finish").invoke(null) }
}

@Implements(className = "com.waz.audioeffect.AudioEffect", isInAndroidSdk = false)
class WireKaliumMeasurementAudioEffectShadow {
    companion object {
        @JvmStatic @Implementation fun __staticInitializer__() = Unit
    }
}
