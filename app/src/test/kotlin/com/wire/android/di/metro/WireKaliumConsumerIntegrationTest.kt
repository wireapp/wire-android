/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.di.metro

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
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
import com.wire.kalium.persistence.db.PlatformDatabaseData
import com.wire.kalium.persistence.db.GlobalDatabaseBuilder
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
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.SQLiteMode
import kotlin.time.Instant

/**
 * The SDK owner, registry, generated consumer graph, repositories and SQLite are real. Public SQL
 * providers seed persisted data before CoreLogic opens it; public account APIs change generations.
 * Only external ports are substituted: no AVS native loading, no WorkManager job execution, and
 * Kalium's existing HTTP mock engine prevents live requests. No client is registered, so native
 * crypto/calling and full network sync are outside this test's boundary.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    application = Application::class,
    sdk = [34],
    instrumentedPackages = ["com.waz.audioeffect"],
    shadows = [WireKaliumIntegrationAudioEffectShadow::class],
)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class WireKaliumConsumerIntegrationTest {
    @Test
    @Suppress("LongMethod") // One retained-owner scenario spans disposal, replacement and final teardown.
    fun givenRealSdkConsumers_whenRetainingClearingAndReplacingAccount_thenFactoryPreservesOwnersAndGenerations() = runBlocking {
        repeat(2) { verifyOwnershipScenario() }
    }

    private suspend fun verifyOwnershipScenario() = coroutineScope {
        val owner = IntegrationOwner()
        val independentOwner = IntegrationOwner()
        val reads = mutableListOf<ActiveReads>()
        var fixture: IntegrationFixture? = null
        mockkObject(WorkManager.Companion)
        try {
            every { WorkManager.getInstance(any<Context>()) } returns mockk(relaxed = true)
            withTimeout(30_000) {
                val sdk = IntegrationFixture.create().also { fixture = it }
                val session = sdk.session()
                val factory = IntegrationFactory(session)
                val provider = ViewModelProvider(owner, factory)
                val first = provider["conversation-screen", WireKaliumIntegrationViewModel::class.java]

                // Recreating the provider, as a retained owner does, must not open another consumer.
                assertSame(
                    first,
                    ViewModelProvider(owner, factory)["conversation-screen", WireKaliumIntegrationViewModel::class.java],
                )
                assertEquals(1, factory.consumers.size)
                val independentProvider = ViewModelProvider(independentOwner, factory)
                val second = independentProvider["conversation-screen", WireKaliumIntegrationViewModel::class.java]
                assertNotSame(first, second)
                assertNotSame(first.consumer, second.consumer)
                assertNotSame(first.message, second.message)
                assertNotSame(first.details, second.details)
                assertEquals(2, factory.consumers.size)
                // Check the actual session wiring, not just that two fresh use cases differ.
                val firstMessageRepository = capture(first.getMessage, "messageRepository")
                assertSame(firstMessageRepository, capture(first.message, "messageRepository"))
                val secondMessageRepository = capture(second.getMessage, "messageRepository")
                assertNotSame(firstMessageRepository, secondMessageRepository)
                assertNotSame(capture(firstMessageRepository, "delegate"), capture(secondMessageRepository, "delegate"))
                assertNotSame(capture(first.details, "conversationRepository"), capture(second.details, "conversationRepository"))

                val firstReads = observe(first, sdk).also(reads::add)
                val secondReads = observe(second, sdk).also(reads::add)
                assertStoredMessage(first.getMessage(sdk.conversationId, MESSAGE_ID))

                owner.viewModelStore.clear()
                firstReads.awaitCancellation()
                assertTrue(first.cleared)
                assertTrue(first.viewModelScope.coroutineContext.job.isCancelled)
                assertRejected { first.getMessage(sdk.conversationId, MESSAGE_ID) }
                assertRejected { firstReads.messageFlow.first() }
                assertRejected { firstReads.detailsFlow.first() }
                assertFalse(second.cleared)
                secondReads.assertActive()
                assertStoredMessage(second.getMessage(sdk.conversationId, MESSAGE_ID))

                // Closing one VM consumer leaves both the parent generation and siblings usable.
                assertSame(session, sdk.session())
                val probe = session.createConsumer()
                try {
                    assertStoredMessage(probe.getMessageById(sdk.conversationId, MESSAGE_ID))
                } finally {
                    probe.closeAndJoin()
                }
                assertTrue(session.coroutineContext.job.isActive)

                // Account replacement uses the real public registry path, not consumer.close().
                // Replacing the generation from a published VM must not wait for that VM's own job.
                val replacementWork = second.viewModelScope.launch(Dispatchers.Default) { sdk.login(replace = true) }
                replacementWork.join()
                assertFalse(replacementWork.isCancelled)
                secondReads.awaitCancellation()
                assertFalse(session.coroutineContext.job.isActive)
                assertRejected { second.getMessage(sdk.conversationId, MESSAGE_ID) }
                assertRejected { secondReads.messageFlow.first() }
                assertRejected { secondReads.detailsFlow.first() }
                assertThrows(IllegalStateException::class.java) { session.createConsumer() }
                assertThrows(IllegalStateException::class.java) {
                    independentProvider["outgoing-new-key", WireKaliumIntegrationViewModel::class.java]
                }
                assertEquals(2, factory.consumers.size)
                assertSame(second, independentProvider["conversation-screen", WireKaliumIntegrationViewModel::class.java])
                assertFalse(second.cleared)
                // Published ViewModels own their consumers; invalidation must not cancel their jobs.
                assertTrue(second.viewModelScope.coroutineContext.job.isActive)

                val replacement = sdk.session()
                assertNotSame(session, replacement)
                assertSame(replacement, sdk.session())
                val freshFactory = IntegrationFactory(replacement)
                val fresh = ViewModelProvider(owner, freshFactory)["conversation-screen", WireKaliumIntegrationViewModel::class.java]
                assertNotSame(first.consumer, fresh.consumer)
                assertNotSame(second.consumer, fresh.consumer)
                assertEquals(1, freshFactory.consumers.size)
                val freshReads = observe(fresh, sdk).also(reads::add)
                assertStoredMessage(fresh.getMessage(sdk.conversationId, MESSAGE_ID))

                // A consumer invalidated after construction must discard the whole unpublished VM.
                val rejectedConsumer = replacement.createConsumer()
                var rejected: WireKaliumIntegrationViewModel? = null
                assertThrows(CancellationException::class.java) {
                    createConsumerViewModel({ rejectedConsumer }) { consumer ->
                        createGraphFactory<WireKaliumIntegrationGraph.Factory>().create(consumer).factory
                            .create(WireKaliumIntegrationViewModel::class, androidx.lifecycle.viewmodel.CreationExtras.Empty)
                            .also { rejected = it; consumer.close() }
                    }
                }
                rejectedConsumer.closeAndJoin()
                assertTrue(checkNotNull(rejected).cleared)
                assertTrue(checkNotNull(rejected).viewModelScope.coroutineContext.job.isCancelled)
                assertTrue(replacement.coroutineContext.job.isActive)
                freshReads.assertActive()
                independentOwner.viewModelStore.clear()
                assertTrue(second.cleared)
                freshReads.assertActive()
                assertStoredMessage(fresh.getMessage(sdk.conversationId, MESSAGE_ID))

                // Real account removal/drain from the VM's scope must return to its terminal caller.
                val terminal = fresh.viewModelScope.async(Dispatchers.Default) { sdk.close() }
                terminal.await()
                fixture = null
                assertTrue(fresh.viewModelScope.coroutineContext.job.isActive)
                freshReads.awaitCancellation()
                owner.viewModelStore.clear()
            }
        } finally {
            owner.viewModelStore.clear()
            independentOwner.viewModelStore.clear()
            try {
                withContext(NonCancellable) {
                    withTimeout(10_000) {
                        reads.forEach { it.cancelAndJoin() }
                        fixture?.close()
                    }
                }
            } finally {
                unmockkObject(WorkManager.Companion)
            }
        }
    }

    private suspend fun CoroutineScope.observe(
        viewModel: WireKaliumIntegrationViewModel,
        sdk: IntegrationFixture,
    ): ActiveReads {
        val messageFlow = viewModel.message(sdk.conversationId, MESSAGE_ID)
        val detailsFlow = viewModel.details(sdk.conversationId)
        val message = CompletableDeferred<ObserveMessageByIdUseCase.Result>()
        val details = CompletableDeferred<ObserveConversationDetailsUseCase.Result>()
        // These collectors deliberately belong to the caller, not viewModelScope. Consumer disposal
        // must cancel admitted resource work even when Android cannot cancel the caller's scope.
        val messageJob = launch(Dispatchers.Default) { messageFlow.collect { message.complete(it) } }
        val detailsJob = launch(Dispatchers.Default) { detailsFlow.collect { details.complete(it) } }
        val observedMessage = (message.await() as ObserveMessageByIdUseCase.Result.Success).message
        assertEquals(MESSAGE_ID, observedMessage.id)
        assertEquals(MESSAGE_TEXT, (observedMessage.content as MessageContent.Text).value)
        val conversation = (details.await() as ObserveConversationDetailsUseCase.Result.Success).conversationDetails.conversation
        assertEquals(sdk.conversationId, conversation.id)
        assertEquals(CONVERSATION_NAME, conversation.name)
        return ActiveReads(messageFlow, detailsFlow, messageJob, detailsJob)
    }

    private fun assertStoredMessage(result: GetMessageByIdUseCase.Result) {
        val message = (result as GetMessageByIdUseCase.Result.Success).message
        assertTrue(message is Message.Regular)
        assertEquals(MESSAGE_ID, message.id)
        assertEquals(MESSAGE_TEXT, (message.content as MessageContent.Text).value)
    }

    private suspend fun assertRejected(block: suspend () -> Unit) {
        // Do not accept a timeout as evidence that a stale operation was rejected.
        val failure = runCatching { block() }.exceptionOrNull()
        assertTrue("Expected consumer cancellation, got $failure", failure is CancellationException)
        if (failure is kotlinx.coroutines.TimeoutCancellationException) throw failure
    }

    private class ActiveReads(
        val messageFlow: Flow<ObserveMessageByIdUseCase.Result>,
        val detailsFlow: Flow<ObserveConversationDetailsUseCase.Result>,
        private val messageJob: Job,
        private val detailsJob: Job,
    ) {
        suspend fun awaitCancellation() {
            messageJob.join()
            detailsJob.join()
            assertTrue(messageJob.isCancelled)
            assertTrue(detailsJob.isCancelled)
        }

        fun assertActive() {
            assertTrue(messageJob.isActive)
            assertTrue(detailsJob.isActive)
        }

        suspend fun cancelAndJoin() {
            messageJob.cancelAndJoin()
            detailsJob.cancelAndJoin()
        }
    }

    private class IntegrationOwner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    private class IntegrationFactory(private val session: UserSessionScope) : WireConsumerMetroViewModelFactory() {
        val consumers = mutableListOf<KaliumConsumer>()
        private val graphFactory = createGraphFactory<WireKaliumIntegrationGraph.Factory>()

        override fun <VM : ViewModel> createInGraph(create: (MetroViewModelFactory) -> VM): VM =
            createConsumerViewModel(
                createConsumer = { session.createConsumer().also(consumers::add) },
            ) { consumer -> create(graphFactory.create(consumer).factory) }
    }

    private class IntegrationFixture(val logic: CoreLogic, val userId: UserId, val conversationId: ConversationId) {
        private val sessions = linkedSetOf<UserSessionScope>()
        private val databases = linkedSetOf<UserDatabaseBuilder>()
        private val networks = linkedSetOf<AuthenticatedNetworkContainer>()
        private var globalDatabase: GlobalDatabaseBuilder? = CoreLogic::class.java
            .getDeclaredMethod("getGlobalDatabaseBuilder").let {
                it.isAccessible = true
                it.invoke(logic) as GlobalDatabaseBuilder
            }

        fun session(): UserSessionScope = logic.getSessionScope(userId).also { session ->
            sessions.add(session)
            networks.add(capture(session, "authenticatedNetworkContainer") as AuthenticatedNetworkContainer)
            val storage = capture(session, "userStorage")
            databases.add(storage.javaClass.getMethod("getDatabase").invoke(storage) as UserDatabaseBuilder)
        }

        suspend fun login(replace: Boolean = false) {
            assertEquals(
                AddAuthenticatedUserUseCase.Result.Success(userId),
                logic.getGlobalScope().addAuthenticatedAccount(
                    StoreSessionParam(
                        serverConfigId = SERVER_CONFIG_ID,
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
            if (globalDatabase == null && databases.isEmpty() && networks.isEmpty()) return
            val removal = runCatching {
                assertEquals(DeleteSessionUseCase.Result.Success, logic.getGlobalScope().deleteSession(userId))
            }
            val cleanup = runCatching {
                withContext(NonCancellable + Dispatchers.Default) {
                    withTimeout(10_000) {
                        logic.getGlobalScope().coroutineContext.job.cancelAndJoin()
                        val detachedJobs = sessions.mapNotNull { session ->
                            val manager = capture(session, "keyPackageManager\$delegate") as Lazy<*>
                            if (!manager.isInitialized()) null else {
                                (capture(checkNotNull(manager.value), "refillKeyPackagesScope") as CoroutineScope).coroutineContext.job
                            }
                        }
                        detachedJobs.forEach { it.cancel() }
                        detachedJobs.forEach { it.join() }
                        sessions.forEach { it.coroutineContext.job.cancel() }
                        sessions.forEach { it.coroutineContext.job.join() }
                        val cellsJobs = sessions.filter { it.isCellsInitialized }.map { it.cells.coroutineContext.job }
                        cellsJobs.forEach { it.cancel() }
                        cellsJobs.forEach { it.join() }
                        assertTrue(cellsJobs.all { it.isCompleted })
                        assertTrue(sessions.all { it.coroutineContext.job.isCompleted })
                        assertTrue(detachedJobs.all { it.isCompleted })
                        while (networks.isNotEmpty()) {
                            val network = networks.first()
                            network.close()
                            networks.remove(network)
                        }
                        while (databases.isNotEmpty()) {
                            val database = databases.first()
                            database.close()
                            databases.remove(database)
                        }
                        globalDatabase?.close()
                        globalDatabase = null
                    }
                }
            }
            removal.exceptionOrNull()?.let { failure ->
                cleanup.exceptionOrNull()?.takeIf { it !== failure }?.let(failure::addSuppressed)
                throw failure
            }
            cleanup.getOrThrow()
        }

        companion object {
            suspend fun create(): IntegrationFixture {
                val context = ApplicationProvider.getApplicationContext<Application>()
                val userId = UserId(UUID.randomUUID().toString(), "consumer-integration.invalid")
                val conversationId = ConversationId("conversation", userId.domain)
                val platform = PlatformDatabaseData(context)
                // No SDK internals or testing hooks: these are the existing public platform providers.
                val global = globalDatabaseProvider(platform, Dispatchers.IO, passphrase = null)
                try {
                    global.serverConfigurationDAO.insert(
                        ServerConfigurationDAO.InsertData(
                            id = SERVER_CONFIG_ID,
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
                return IntegrationFixture(logic, userId, conversationId).also {
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
        const val SERVER_CONFIG_ID = "wire-consumer-integration"
        const val URL = "https://consumer-integration.invalid"
        const val CONVERSATION_NAME = "Persisted integration conversation"
        const val MESSAGE_ID = "persisted-integration-message"
        const val MESSAGE_TEXT = "A real SQLite message reached the Android ViewModel"
    }
}

internal class WireKaliumIntegrationViewModel @Inject constructor(
    val consumer: KaliumConsumer,
    val details: ObserveConversationDetailsUseCase,
    val message: ObserveMessageByIdUseCase,
    val getMessage: GetMessageByIdUseCase,
) : ViewModel() {
    var cleared = false
        private set

    override fun onCleared() {
        cleared = true
    }
}

@DependencyGraph(bindingContainers = [KaliumConsumerModule::class, WireKaliumIntegrationBindings::class])
internal interface WireKaliumIntegrationGraph : MetroViewModelMultibindings {
    val factory: WireViewModelGraphFactory

    @DependencyGraph.Factory
    interface Factory {
        fun create(@Provides consumer: KaliumConsumer): WireKaliumIntegrationGraph
    }
}

@BindingContainer
internal object WireKaliumIntegrationBindings {
    @Provides
    @IntoMap
    @ViewModelKey(WireKaliumIntegrationViewModel::class)
    fun viewModel(viewModel: WireKaliumIntegrationViewModel): ViewModel = viewModel
}

/** CoreLogic eagerly creates AudioEffect; its static initializer calls AVSystem.load even with calling disabled. */
@Implements(className = "com.waz.audioeffect.AudioEffect", isInAndroidSdk = false)
@Suppress("UtilityClassWithPublicConstructor") // Robolectric instantiates shadows through a public constructor.
class WireKaliumIntegrationAudioEffectShadow {
    companion object {
        @JvmStatic
        @Implementation
        fun __staticInitializer__() = Unit
    }
}

// Read existing captures without widening SDK visibility or initializing unused lazy resources.
private fun capture(owner: Any, name: String): Any = owner.javaClass.getDeclaredField(name).let {
    it.isAccessible = true
    checkNotNull(it.get(owner))
}
