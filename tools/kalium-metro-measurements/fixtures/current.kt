/* Ignored measurement fixture, derived from the verified WireKaliumConsumerIntegrationTest.
 * Parent stages exactly one API variant; this file has not been Gradle-compiled by its author.
 */
// The harness and source manifest intentionally address this lowercase template filename.
@file:Suppress("Filename")

package com.wire.android.di.metro

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
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
import com.wire.kalium.network.networkContainer.AuthenticatedNetworkContainer
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
import com.wire.kalium.persistence.db.UserDatabaseBuilder
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
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID
import java.util.function.Supplier
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
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
    // Keep measurement windows and failure-safe teardown in one orchestration method.
    // Literal timeouts are fixed test limits; preserve their values and placement.
    @Suppress("LongMethod", "MagicNumber")
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
        measurementFinally({
            every { WorkManager.getInstance(any<Context>()) } returns mockk(relaxed = true)
            repeat(warmups + repetitions) { iteration ->
                val warming = iteration < warmups
                val owners = mutableListOf<MeasurementOwner>()
                var fixture: MeasurementFixture? = null
                var factory: MeasurementFactory? = null
                var firstConsumer: KaliumConsumer? = null
                measurementFinally({
                    withTimeout(90_000) {
                        // Seed/global CoreLogic/account registration excluded from SESSION measurement.
                        val sdk = MeasurementFixture.create().also { fixture = it }
                        sdk.sessionResolutionStarted = true
                        val session = Meter.measure("cold_session_get_or_create", iteration, warming) {
                            sdk.logic.getSessionScope(sdk.userId)
                        }
                        // Assignment only, immediately AFTER the cold probe; never reopen an owner in cleanup.
                        sdk.retainSession(session)
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
                                            try {
                                                nextOwner.viewModelStore.clear()
                                            } finally {
                                                dispatcher.drainCreatedConsumer()
                                            }
                                        }
                                    }
                                }
                            }
                            dispatcher.assertCreated(1 + warmVms)
                            assertSame(session, sdk.logic.getSessionScope(sdk.userId))
                        }
                    }
                }) {
                    withContext(NonCancellable) {
                        measurementFinally({
                            withTimeout(30_000) {
                                try {
                                    owners.forEach { it.viewModelStore.clear() }
                                } finally {
                                    try {
                                        firstConsumer?.closeAndJoin()
                                    } finally {
                                        factory?.drainCreatedConsumer()
                                    }
                                }
                            }
                        }) {
                            firstConsumer = null
                            fixture?.close() // Own independent deadline/NonCancellable context even after consumer failure.
                        }
                    }
                }
            }
        }) {
            measurementFinally({ Meter.finish() }) { unmockkObject(WorkManager.Companion) }
        }
    }

    private suspend fun validateReads(vm: MeasurementViewModel, sdk: MeasurementFixture) {
        val message = (
            vm.message(sdk.conversationId, MESSAGE_ID)
                .first { it is ObserveMessageByIdUseCase.Result.Success } as ObserveMessageByIdUseCase.Result.Success
        ).message
        assertEquals(MESSAGE_ID, message.id)
        assertEquals(MESSAGE_TEXT, (message.content as MessageContent.Text).value)
        val details = (
            vm.details(sdk.conversationId)
                .first { it is ObserveConversationDetailsUseCase.Result.Success } as ObserveConversationDetailsUseCase.Result.Success
        ).conversationDetails.conversation
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
                session.createConsumer().also {
                    createdConsumer = it
                    created++
                }
            }) { consumer ->
                create(graphFactory.create(consumer).factory)
            }
        }
        fun assertCreated(expected: Int) { assertEquals(expected, created) }
        fun takeCreatedConsumer(): KaliumConsumer? = createdConsumer.also { createdConsumer = null }
        suspend fun drainCreatedConsumer() { takeCreatedConsumer()?.closeAndJoin() }
    }

    internal class MeasurementFixture private constructor(
        val logic: CoreLogic,
        val userId: UserId,
        val conversationId: ConversationId,
        val configId: String,
        private val networkObserver: MeasurementNetworkObserver,
    ) {
        internal val networkObserverJob: Job = requireNotNull(networkObserver.job)
        var sessionResolutionStarted = false
        private var retainedSession: UserSessionScope? = null

        fun retainSession(session: UserSessionScope) {
            check(retainedSession == null) { "Only one account generation is allowed per metric cycle" }
            retainedSession = session
        }

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

        suspend fun close() = withContext(NonCancellable + Dispatchers.Default) {
            measurementFinally({
                // A failed cold resolution may have acquired resources without returning its owner.
                // Do not create a replacement owner or close storage whose quiescence cannot be proved.
                check(!sessionResolutionStarted || retainedSession != null) {
                    "Cold session resolution did not return a retainable owner"
                }
                val session = retainedSession?.let(::RetainedMeasurementSession)
                val globalJob = logic.getGlobalScope().coroutineContext.job
                val getter = CoreLogic::class.java.getDeclaredMethod("getGlobalDatabaseBuilder")
                getter.isAccessible = true
                val globalDatabase = getter.invoke(logic) as GlobalDatabaseBuilder
                // Capture actual session resources BEFORE deleteSession evicts provider entries.
                MeasurementOwnerDrain(
                    globalJob = globalJob,
                    sessionJob = session?.job,
                    initializedCellsJob = { session?.initializedCellsJob() },
                    stopAccountWork = { session?.stopAccountWork() },
                    evict = { assertEquals(DeleteSessionUseCase.Result.Success, logic.getGlobalScope().deleteSession(userId)) },
                    closeUserResources = {
                        networkObserver.close()
                        session?.closeResources()
                    },
                    closeGlobalResource = { globalDatabase.close() },
                ).close()
            }) {
                // Also release the observer after failed resource capture/eviction/drain; never close live storage.
                networkObserver.close()
            }
        }

        companion object {
            // Setup failure, including cancellation, must drain captured owners before rethrowing.
            @Suppress("LongMethod", "TooGenericExceptionCaught")
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
                val networkObserver = MeasurementNetworkObserver(context)
                var fixture: MeasurementFixture? = null
                try {
                    val logic = networkObserver.createLogic {
                        CoreLogic(
                            userAgent = "wire-consumer-integration-test",
                            appContext = context,
                            rootPath = context.filesDir.absolutePath,
                            kaliumConfigs = KaliumConfigs(
                                enableCalling = false,
                                shouldEncryptData = { false },
                                mockedRequests = emptyList(),
                            ),
                        )
                    }
                    val created = MeasurementFixture(logic, userId, conversationId, configId, networkObserver)
                        .also { fixture = it }
                    created.login()
                    return created
                } catch (failure: Throwable) {
                    return measurementFinally({ throw failure }) {
                        withContext(NonCancellable + Dispatchers.Default) {
                            fixture?.close() ?: networkObserver.close()
                        }
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

    @DependencyGraph.Factory
    interface Factory {
        fun create(@Provides consumer: KaliumConsumer): MeasurementGraph
    }
}

@BindingContainer
internal object MeasurementBindings {
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
@Suppress("UtilityClassWithPublicConstructor") // Robolectric instantiates shadows through a public constructor.
class WireKaliumMeasurementAudioEffectShadow {
    companion object {
        @JvmStatic
        @Implementation
        @Suppress("FunctionNaming") // Robolectric requires this exact static-initializer hook name.
        fun __staticInitializer__() = Unit
    }
}

// Shared verbatim by the two fixture copies. This is measurement-fixture code, not an SDK hook.
// Eviction failure must remain primary while safe cleanup is attempted.
@Suppress("TooGenericExceptionCaught")
internal class MeasurementOwnerDrain(
    private val globalJob: Job,
    private val sessionJob: Job?,
    private val initializedCellsJob: () -> Job?,
    private val stopAccountWork: suspend () -> Unit,
    private val evict: suspend () -> Unit,
    private val closeUserResources: suspend () -> Unit,
    private val closeGlobalResource: () -> Unit,
) {
    suspend fun close(): Unit = withTimeout(MEASUREMENT_CLEANUP_TIMEOUT_MS) {
        // Eviction failure still requires an attempt to drain the retained owners, and remains fatal.
        var evictionFailure: Throwable? = null
        try {
            evict()
        } catch (failure: Throwable) {
            currentCoroutineContext().ensureActive()
            evictionFailure = failure
        }
        try {
            // No global observer may reopen account work while its captured resources are closing.
            globalJob.cancelAndJoin()
            try {
                // Reuse the SDK's Cells -> session -> Cells join -> calling (/consumer) ordering.
                stopAccountWork()
            } catch (timeout: TimeoutCancellationException) {
                // An SDK best-effort timeout is not quiescence. Do not swallow our own deadline.
                currentCoroutineContext().ensureActive()
            }
            sessionJob?.cancelAndJoin()
            initializedCellsJob()?.cancelAndJoin()
            // The SDK timeout may have skipped calling or consumer drain. Finish those steps now
            // that the independently retained session and Cells jobs have really completed.
            stopAccountWork()
            requireMeasurementQuiescence(globalJob)
            sessionJob?.let(::requireMeasurementQuiescence)
            initializedCellsJob()?.let(::requireMeasurementQuiescence)
            // Deliberately not finally blocks: failed/timed-out drain must never close live storage.
            closeUserResources()
            closeGlobalResource()
        } catch (failure: Throwable) {
            val primary = evictionFailure ?: throw failure
            if (primary !== failure) primary.addSuppressed(failure)
            throw primary
        }
        evictionFailure?.let { throw it }
    }
}

private const val MEASUREMENT_CLEANUP_TIMEOUT_MS = 30_000L

private fun requireMeasurementQuiescence(job: Job) {
    check(job.isCompleted && job.children.none()) { "Measurement owner still retains work: $job" }
}

private class RetainedMeasurementSession(private val session: UserSessionScope) {
    val job: Job = session.coroutineContext.job

    // Both SDK versions acquire these fields eagerly in UserSessionScope construction. Reading
    // their fields cannot resolve a new owner or initialize an unused API graph/lazy resource.
    private val storage = measurementField(session, "userStorage")
    private val database = storage.javaClass.getMethod("getDatabase").invoke(storage) as UserDatabaseBuilder
    private val network = measurementField(session, "authenticatedNetworkContainer") as AuthenticatedNetworkContainer
    private val cryptoDelegates = listOf("proteusClientProviderDelegate", "mlsClientProviderDelegate")
        .map { measurementField(session, it) as Lazy<*> }

    fun initializedCellsJob(): Job? =
        if (session.isCellsInitialized) session.cells.coroutineContext.job else null

    suspend fun stopAccountWork() = measurementSuspend(session, "stopAccountWork")

    suspend fun closeResources() {
        // Preserve closeAccountResources' native -> network -> database order without re-resolving
        // resources after provider eviction. Never force an unused crypto/Cells/calling lazy.
        cryptoDelegates.forEach { delegate ->
            if (delegate.isInitialized()) measurementSuspend(requireNotNull(delegate.value), "close")
        }
        network.close()
        database.close()
    }
}

private fun measurementField(owner: Any, name: String): Any = owner.javaClass.getDeclaredField(name).let {
    it.isAccessible = true
    requireNotNull(it.get(owner)) { "Missing acquired measurement resource: $name" }
}

private suspend fun measurementSuspend(owner: Any, baseName: String): Unit =
    suspendCoroutineUninterceptedOrReturn { continuation ->
        // JVM internal suffixes belong to the emitting module, not the source API. Match only the
        // inspected zero-argument suspend signature, with an optional internal module suffix.
        val method = owner.javaClass.methods.single {
            (it.name == baseName || it.name.startsWith(baseName + "$")) &&
                it.parameterTypes.contentEquals(arrayOf(Continuation::class.java))
        }
        method.isAccessible = true
        try {
            method.invoke(owner, continuation)
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
    }

private const val MEASUREMENT_NETWORK_OBSERVER_CLASS = "com.wire.kalium.logic.network.NetworkStateObserverImpl"

/** Snapshots are detached identity sets; never mutate Robolectric's static callback registry. */
private fun measurementNetworkCallbacks(manager: ConnectivityManager): Set<ConnectivityManager.NetworkCallback> =
    Collections.newSetFromMap(IdentityHashMap<ConnectivityManager.NetworkCallback, Boolean>()).apply {
        addAll(Shadows.shadowOf(manager).networkCallbacks)
    }

/** Both inspected SDK callback classes capture this exact observer in the synthetic this$0 field. */
private fun measurementCallbackObserver(callback: ConnectivityManager.NetworkCallback): Any? =
    if (callback.javaClass.name == "$MEASUREMENT_NETWORK_OBSERVER_CLASS\$callback\$1") {
        measurementField(callback, "this\$0")
    } else {
        null
    }

/** Own only the observer eagerly created during this fixture's CoreLogic constructor. */
private class MeasurementNetworkObserver(context: Context) {
    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var initialCallbacks = emptySet<ConnectivityManager.NetworkCallback>()
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var released = false
    var job: Job? = null
        private set

    fun createLogic(create: () -> CoreLogic): CoreLogic {
        // No session/account setup belongs between these two snapshots.
        initialCallbacks = measurementNetworkCallbacks(manager)
        var logic: CoreLogic? = null
        return measurementFinally({
            create().also { logic = it }
        }) {
            capture(logic)
        }
    }

    private fun capture(logic: CoreLogic?) {
        val after = measurementNetworkCallbacks(manager)
        val added = after.filterNot(initialCallbacks::contains)
        // A later CoreLogic initializer (e.g. AVS) can throw after callback registration.
        // Recover only the new callback's inspected outer observer in that failure case.
        val observer = logic?.networkStateObserver ?: added.singleOrNull()?.let(::measurementCallbackObserver)
        if (observer != null) {
            check(observer.javaClass.name == MEASUREMENT_NETWORK_OBSERVER_CLASS) { "Unexpected eager network observer" }
            job = (measurementField(observer, "scope") as CoroutineScope).coroutineContext.job
            callback = added.singleOrNull { measurementCallbackObserver(it) === observer }
        }
        check(after.containsAll(initialCallbacks) && added.size == 1 && callback != null && job != null) {
            "Cannot prove exact CoreLogic network callback/scope ownership"
        }
    }

    suspend fun close(): Unit = withTimeout(MEASUREMENT_CLEANUP_TIMEOUT_MS) {
        if (!released) {
            // Even unregister failure must attempt the independent scope drain, retaining both failures.
            measurementFinally({
                callback?.let {
                    manager.unregisterNetworkCallback(it)
                    callback = null
                }
            }) {
                job?.cancelAndJoin()
                job?.let(::requireMeasurementQuiescence)
                check(measurementNetworkCallbacks(manager) == initialCallbacks) {
                    "Network callbacks differ from the pre-CoreLogic snapshot"
                }
            }
            check(job != null) { "Eager network observer scope was not captured" }
            // Do not retain callbacks (including unrelated initial callbacks) across fixture cycles.
            initialCallbacks = emptySet()
            released = true
        }
    }
}

/** Cleanup remains fatal without replacing an earlier setup, measurement or drain failure. */
@Suppress("TooGenericExceptionCaught")
private inline fun <T> measurementFinally(action: () -> T, cleanup: () -> Unit): T {
    var primary: Throwable? = null
    try {
        return action()
    } catch (failure: Throwable) {
        primary = failure
        throw failure
    } finally {
        try {
            cleanup()
        } catch (failure: Throwable) {
            val original = primary ?: throw failure
            if (original !== failure) original.addSuppressed(failure)
        }
    }
}

/** Separate invocation: real CoreLogic/session adapter cycles, with no Meter or bootstrap Probe. */
@RunWith(RobolectricTestRunner::class)
@Config(
    application = Application::class,
    sdk = [34],
    instrumentedPackages = ["com.waz.audioeffect"],
    shadows = [WireKaliumMeasurementAudioEffectShadow::class],
)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class WireKaliumMeasurementNetworkObserverTest {
    @Suppress("LongMethod", "MagicNumber")
    @Test
    fun twoOrdinaryFixtureCyclesDrainTheEagerNetworkObserver() = runBlocking {
        mockkObject(WorkManager.Companion)
        measurementFinally({
            every { WorkManager.getInstance(any<Context>()) } returns mockk(relaxed = true)
            val context = ApplicationProvider.getApplicationContext<Application>()
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val originalCallbacks = measurementNetworkCallbacks(manager)
            // A foreign registration proves fixture cleanup cannot clear/reset the callback registry.
            val unrelated = object : ConnectivityManager.NetworkCallback() {}
            manager.registerDefaultNetworkCallback(unrelated)
            measurementFinally({
                val cycleCallbacks = measurementNetworkCallbacks(manager)
                assertTrue(cycleCallbacks.contains(unrelated))
                var previousJob: Job? = null
                repeat(2) {
                    var fixture: WireKaliumMeasurementTest.MeasurementFixture? = null
                    var observerJob: Job? = null
                    measurementFinally({
                        withTimeout(90_000L) {
                            val sdk = WireKaliumMeasurementTest.MeasurementFixture.create().also { fixture = it }
                            val capturedJob = sdk.networkObserverJob.also { observerJob = it }
                            assertTrue(capturedJob.isActive)
                            assertTrue(capturedJob !== previousJob)
                            assertEquals(cycleCallbacks.size + 1, measurementNetworkCallbacks(manager).size)
                            sdk.sessionResolutionStarted = true
                            val session = sdk.logic.getSessionScope(sdk.userId)
                            sdk.retainSession(session)
                            assertSame(session, sdk.logic.getSessionScope(sdk.userId))
                        }
                    }) {
                        fixture?.close()
                    }
                    val capturedJob = requireNotNull(observerJob)
                    requireMeasurementQuiescence(capturedJob)
                    assertEquals(cycleCallbacks, measurementNetworkCallbacks(manager))
                    previousJob = capturedJob
                }
            }) {
                manager.unregisterNetworkCallback(unrelated)
                assertEquals(originalCallbacks, measurementNetworkCallbacks(manager))
            }
        }) {
            unmockkObject(WorkManager.Companion)
        }
    }
}

/** Run separately BEFORE cohorts. Uses the exact MeasurementOwnerDrain embedded in either fixture. */
// Virtual deadlines and the held-owner orchestration are intentional regression fixtures.
@Suppress("LongMethod", "MagicNumber")
@OptIn(ExperimentalCoroutinesApi::class)
class WireKaliumMeasurementOwnerDrainTest {
    @Test
    fun cancelOnlyEvictionCannotOutrunRetainedSessionChild() = heldOwnerBarrier(providerWaits = false)

    @Test
    fun bestEffortProviderTimeoutCannotOutrunRetainedSessionChild() = heldOwnerBarrier(providerWaits = true)

    @Test
    fun independentlyOwnedInitializedCellsMustAlsoDrain() = heldOwnerBarrier(providerWaits = true, holdCells = true)

    private fun heldOwnerBarrier(providerWaits: Boolean, holdCells: Boolean = false) = runTest {
        val global = Job()
        val session = Job() // Deliberately NOT a child of global: this is the SDK's actual topology.
        val cells = if (holdCells) Job() else null
        val heldOwner = cells ?: session
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val evicted = CompletableDeferred<Unit>()
        val closes = mutableListOf<String>()
        var shutdownCalls = 0
        val child = CoroutineScope(heldOwner + StandardTestDispatcher(testScheduler)).launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                entered.complete(Unit)
                release.await()
                finished.complete(Unit)
            }
        }
        val closing = async {
            MeasurementOwnerDrain(
                globalJob = global,
                sessionJob = session,
                initializedCellsJob = { cells },
                stopAccountWork = {
                    shutdownCalls++
                    withTimeout(3_000) {
                        cells?.cancel()
                        session.cancelAndJoin()
                        cells?.join()
                    }
                },
                evict = {
                    session.cancel()
                    if (providerWaits) {
                        try {
                            withTimeout(3_000) { heldOwner.join() }
                        } catch (_: TimeoutCancellationException) {
                            // Reproduce the provider's legacy best-effort timeout, not the helper's.
                        }
                    }
                    evicted.complete(Unit)
                },
                closeUserResources = {
                    check(finished.isCompleted && heldOwner.isCompleted && child.isCompleted)
                    closes.add("network")
                    closes.add("user-database")
                },
                closeGlobalResource = { closes.add("global-database") },
            ).close()
        }
        try {
            entered.await()
            // Pass BOTH the provider's and the first stopAccountWork's 3s virtual deadlines.
            // No sleep/race window: the held NonCancellable child is the only release mechanism.
            advanceTimeBy(6_001)
            runCurrent()
            assertTrue(evicted.isCompleted)
            assertTrue(global.isCompleted)
            assertFalse(child.isCompleted)
            assertFalse("Close returned while an evicted owner still retained work", closing.isCompleted)
            assertEquals(emptyList<String>(), closes)
            release.complete(Unit)
            closing.await()
            assertTrue(child.isCompleted)
            assertTrue(session.isCompleted)
            assertTrue(cells == null || cells.isCompleted)
            assertTrue("Post-timeout calling/consumer steps were not revisited", shutdownCalls >= 2)
            assertEquals(listOf("network", "user-database", "global-database"), closes)
        } finally {
            release.complete(Unit)
            withContext(NonCancellable) {
                closing.cancelAndJoin()
                session.cancelAndJoin()
                cells?.cancelAndJoin()
                global.cancelAndJoin()
            }
        }
    }

    @Test
    fun cleanupDeadlineFailsWithoutClosingAnyCapturedResource() = runTest {
        val global = Job()
        val session = Job()
        val release = CompletableDeferred<Unit>()
        var closed = false
        val child = CoroutineScope(session + StandardTestDispatcher(testScheduler)).launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) { release.await() }
        }
        val closing = async {
            runCatching {
                MeasurementOwnerDrain(global, session, { null }, {}, { session.cancel() }, { closed = true }, { closed = true }).close()
            }
        }
        try {
            advanceTimeBy(30_001)
            runCurrent()
            assertTrue(closing.await().exceptionOrNull() is TimeoutCancellationException)
            assertFalse(closed)
            assertFalse(child.isCompleted)
        } finally {
            release.complete(Unit)
            withContext(NonCancellable) {
                closing.cancelAndJoin()
                session.cancelAndJoin()
                global.cancelAndJoin()
            }
        }
    }

    @Test
    fun evictionFailureRemainsFatalAfterSafeResourceCleanup() = runTest {
        val failure = IllegalStateException("metadata deletion failed")
        var closed = false
        val result = runCatching {
            MeasurementOwnerDrain(Job(), Job(), { null }, {}, { throw failure }, { closed = true }, {}).close()
        }
        assertSame(failure, result.exceptionOrNull())
        assertTrue(closed)
    }

    @Test
    fun resourceCloseFailureCannotReportSuccessfulCleanup() = runTest {
        val failure = IllegalStateException("captured network close failed")
        var globalClosed = false
        val result = runCatching {
            MeasurementOwnerDrain(Job(), Job(), { null }, {}, {}, { throw failure }, { globalClosed = true }).close()
        }
        assertSame(failure, result.exceptionOrNull())
        assertFalse(globalClosed)
    }
}
