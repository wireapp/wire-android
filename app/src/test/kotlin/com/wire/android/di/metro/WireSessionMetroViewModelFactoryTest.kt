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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.media.audiomessage.AudioMessageArgs
import com.wire.android.media.audiomessage.AudioMessageViewModelImpl
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.ui.authentication.SessionAuthenticationManualViewModelFactory
import com.wire.android.ui.authentication.SessionAuthenticationViewModelFactory
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.home.conversations.ScopedMessageManualViewModelFactory
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.di.KaliumConsumer
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageByIdUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.MetroViewModelMultibindings
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class WireSessionMetroViewModelFactoryTest {
    @Test
    fun givenPublishedViewModel_whenConsumerCloses_thenTerminalCallerAndFollowupContinue() = runTest {
        val arrangement = Arrangement()
        val owner = Owner()
        val viewModel = ViewModelProvider(owner, arrangement.factory)[DirectConsumerViewModel::class.java]
        val consumer = arrangement.consumers.single()
        val teardown = backgroundScope.async(start = CoroutineStart.LAZY) { consumer.closeAndJoin() }
        var continued = false
        val terminal = viewModel.viewModelScope.launch {
            // The SDK's global logout orchestration invalidates and drains the consumer, then the
            // ordinary cancellable profile caller must resume its store/navigation work.
            teardown.start()
            teardown.await()
            continued = true
        }
        try {
            runCurrent()
            assertTrue(terminal.isCompleted)
            assertFalse(terminal.isCancelled)
            assertTrue(continued)
            assertTrue(viewModel.viewModelScope.coroutineContext[Job]!!.isActive)
            assertFalse(viewModel.cleared)
            assertEquals(0, viewModel.ownedResource.closes)
            assertEquals(0, arrangement.parent.closes)
        } finally {
            owner.viewModelStore.clear()
        }
        assertTrue(viewModel.viewModelScope.coroutineContext[Job]!!.isCancelled)
        assertEquals(1, viewModel.ownedResource.closes)
        assertEquals(1, consumer.closes)
    }

    @Test
    fun givenFactoryCreatedClearSessionViewModel_whenCancellingLogin_thenNonCancellableLogoutAndDeletionComplete() = runTest {
        val arrangement = Arrangement()
        val owner = Owner()
        val manualFactory = arrangement.factory.wireManualViewModelFactory(SessionAuthenticationManualViewModelFactory::class) {
            clearSessionViewModel(arrangement.terminalActions.userId)
        }
        val viewModel = ViewModelProvider(owner, manualFactory)[ClearSessionViewModel::class.java]
        val consumer = arrangement.consumers.single()
        val followedUp = CompletableDeferred<Unit>()
        val teardown = backgroundScope.async(start = CoroutineStart.LAZY) { consumer.closeAndJoin() }
        coEvery { arrangement.terminalActions.logout(LogoutReason.SELF_HARD_LOGOUT, true) } coAnswers {
            // Mirror the actual LogoutUseCase's independent global orchestration; this test covers
            // the actual Android factory and cancelLogin, not the real SDK provider (covered on JVM).
            teardown.start()
            teardown.await()
        }
        coEvery { arrangement.terminalActions.deleteSession(arrangement.terminalActions.userId) } answers {
            followedUp.complete(Unit)
            DeleteSessionUseCase.Result.Success
        }
        try {
            viewModel.onCancelLoginClicked(arrangement.terminalActions.switchActions)
            runCurrent()
            assertTrue(teardown.isCompleted)
            assertTrue(followedUp.isCompleted)
            assertTrue(viewModel.viewModelScope.coroutineContext[Job]!!.isActive)
            coVerify(exactly = 1) {
                arrangement.terminalActions.logout(LogoutReason.SELF_HARD_LOGOUT, true)
                arrangement.terminalActions.deleteSession(arrangement.terminalActions.userId)
                arrangement.terminalActions.switchAccount(SwitchAccountParam.TryToSwitchToNextAccount)
            }
            arrangement.relogin()
            val reopenedProvider = ViewModelProvider(owner, arrangement.createSessionFactory())
            val reopened = reopenedProvider["reopened", DirectConsumerViewModel::class.java]
            assertSame(arrangement.replacementConsumers.single().observeMessageById, reopened.message)
            assertEquals(0, arrangement.parent.closes)
        } finally {
            owner.viewModelStore.clear()
        }
    }

    @Test
    fun givenSameAccountRelogin_whenUsingRetainedSessionFactory_thenItCannotSwitchToTheNewGeneration() {
        val arrangement = Arrangement()
        val owner = Owner()
        val provider = ViewModelProvider(owner, arrangement.factory)
        val retained = provider["retained", DirectConsumerViewModel::class.java]
        arrangement.relogin()

        assertSame(retained, provider["retained", DirectConsumerViewModel::class.java])
        assertThrows(IllegalStateException::class.java) {
            provider["new-key", DirectConsumerViewModel::class.java]
        }
        assertEquals(1, arrangement.consumers.size)
        assertEquals(0, arrangement.replacementConsumers.size)
        arrangement.verifySessionLookups(1)

        val freshFactory = arrangement.createSessionFactory()
        val fresh = ViewModelProvider(owner, freshFactory)["fresh-generation", DirectConsumerViewModel::class.java]
        assertSame(arrangement.replacementConsumers.single().observeMessageById, fresh.message)
        arrangement.verifySessionLookups(2)
        owner.viewModelStore.clear()
    }

    @Test
    fun givenInvalidationDuringChildGraphCreation_whenCreatingViewModel_thenConsumerCannotPublishIt() {
        val arrangement = Arrangement()
        arrangement.graphHook = arrangement::relogin

        assertThrows(CancellationException::class.java) {
            arrangement.factory.create(DirectConsumerViewModel::class, CreationExtras.Empty)
        }

        assertEquals(1, arrangement.childGraphs)
        assertEquals(1, arrangement.consumers.single().closes)
        assertTrue(arrangement.replacementConsumers.isEmpty())
        assertEquals(0, arrangement.parent.closes)
        arrangement.verifySessionLookups(1)
    }

    @Test
    fun givenInvalidationDuringManualCallback_whenViewModelHasBeenConstructed_thenItIsClearedAndNotPublished() {
        val arrangement = Arrangement()
        val owner = Owner()
        var unpublished: ManualConsumerViewModel? = null
        val manualFactory = arrangement.factory.wireManualViewModelFactory(TestManualFactory::class) {
            create("manual", null).also {
                unpublished = it
                arrangement.relogin()
            }
        }

        assertThrows(CancellationException::class.java) {
            ViewModelProvider(owner, manualFactory)["manual-key", ManualConsumerViewModel::class.java]
        }

        assertTrue(unpublished!!.cleared)
        assertEquals(1, arrangement.consumers.single().closes)
        assertTrue(arrangement.replacementConsumers.isEmpty())
        assertEquals(0, arrangement.parent.closes)
        owner.viewModelStore.clear()
    }

    @Test
    fun givenInvalidationDuringBlockedViewModelAssembly_whenAssemblyReturns_thenUnpublishedViewModelIsCleared() = runBlocking {
        val arrangement = Arrangement()
        val entered = CountDownLatch(1)
        val finishAssembly = CountDownLatch(1)
        val finishCleanup = CompletableDeferred<Unit>()
        val cleanupEntered = CompletableDeferred<Unit>()
        val threads = Executors.newSingleThreadExecutor()
        var assembled: DirectConsumerViewModel? = null
        var ownedWork: Job? = null
        arrangement.hooks.onDirectCreate = { viewModel ->
            assembled = viewModel
            ownedWork = viewModel.viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) {
                        cleanupEntered.complete(Unit)
                        finishCleanup.await()
                    }
                }
            }
            entered.countDown()
            check(finishAssembly.await(5, TimeUnit.SECONDS))
        }
        val owner = Owner()
        try {
            val result = threads.submit(
                Callable {
                    runCatching {
                        ViewModelProvider(owner, arrangement.factory)["blocked", DirectConsumerViewModel::class.java]
                    }
                }
            )
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            arrangement.relogin()
            val consumer = arrangement.consumers.single()
            val drain = async(start = CoroutineStart.UNDISPATCHED) { consumer.closeAndJoin() }
            assertFalse(drain.isCompleted)
            finishAssembly.countDown()

            assertTrue(result.get(5, TimeUnit.SECONDS).exceptionOrNull() is CancellationException)
            cleanupEntered.await()
            assertTrue(assembled!!.cleared)
            assertEquals(1, assembled!!.ownedResource.closes)
            assertTrue(ownedWork!!.isCancelled)
            assertFalse(drain.isCompleted)
            assertEquals(1, consumer.closes)
            assertEquals(0, arrangement.parent.closes)
            assertTrue(arrangement.replacementConsumers.isEmpty())
            arrangement.verifySessionLookups(1)

            finishCleanup.complete(Unit)
            drain.await()
            assertTrue(ownedWork!!.isCompleted)
            // A failed factory result must not occupy the caller's key.
            arrangement.hooks.onDirectCreate = {}
            val freshProvider = ViewModelProvider(owner, arrangement.createSessionFactory())
            val fresh = freshProvider["blocked", DirectConsumerViewModel::class.java]
            assertNotSame(assembled, fresh)
        } finally {
            finishAssembly.countDown()
            finishCleanup.complete(Unit)
            threads.shutdownNow()
            owner.viewModelStore.clear()
        }
    }

    @Test
    fun givenRetainedViewModel_whenLookingUpWithSameOwnerAndKey_thenNoNewConsumerOrGraphIsCreated() {
        val arrangement = Arrangement()
        assertEquals(0, arrangement.consumers.size)
        assertEquals(0, arrangement.childGraphs)
        val owner = Owner()
        val first = ViewModelProvider(owner, arrangement.factory)["retained", DirectConsumerViewModel::class.java]

        // Recreating the provider/factory at a Compose call site must be harmless on a store hit.
        val retained = ViewModelProvider(owner, arrangement.factory)["retained", DirectConsumerViewModel::class.java]

        assertSame(first, retained)
        assertEquals(1, arrangement.consumers.size)
        assertEquals(1, arrangement.childGraphs)
        assertEquals(0, arrangement.consumers.single().closes)
        owner.viewModelStore.clear()
    }

    @Test
    fun givenDifferentKeys_whenCreatingViewModels_thenConsumersDifferAndParentIsShared() {
        val arrangement = Arrangement()
        val owner = Owner()
        val provider = ViewModelProvider(owner, arrangement.factory)
        val first = provider["first", DirectConsumerViewModel::class.java]
        val second = provider["second", DirectConsumerViewModel::class.java]

        assertEquals(2, arrangement.consumers.size)
        assertNotSame(first.details, second.details)
        assertNotSame(first.message, second.message)
        assertNotSame(first.getMessage, second.getMessage)
        assertSame(arrangement.consumers[0].observeConversationDetails, first.details)
        assertSame(arrangement.consumers[0].observeMessageById, first.message)
        assertSame(arrangement.consumers[0].getMessageById, first.getMessage)
        assertSame(first.parent, second.parent)
        assertSame(arrangement.parent, first.parent)
        owner.viewModelStore.clear()
    }

    @Test
    fun givenDifferentOwnersWithSameKey_whenClearingOneOwner_thenOnlyItsConsumerCloses() {
        val arrangement = Arrangement()
        val firstOwner = Owner()
        val secondOwner = Owner()
        ViewModelProvider(firstOwner, arrangement.factory)["same", DirectConsumerViewModel::class.java]
        ViewModelProvider(secondOwner, arrangement.factory)["same", DirectConsumerViewModel::class.java]

        firstOwner.viewModelStore.clear()
        firstOwner.viewModelStore.clear()

        assertEquals(1, arrangement.consumers[0].closes)
        assertEquals(0, arrangement.consumers[1].closes)
        assertEquals(0, arrangement.parent.closes)
        secondOwner.viewModelStore.clear()
        assertEquals(1, arrangement.consumers[1].closes)
        assertEquals(0, arrangement.parent.closes)
    }

    @Test
    fun givenAssistedBinding_whenCreatingViewModel_thenChildDependenciesAndExtrasAreUsed() {
        val arrangement = Arrangement()
        val extras = MutableCreationExtras().apply { this[ARGUMENT] = "assisted" }
        val owner = Owner()
        val viewModel = ViewModelProvider.create(owner, arrangement.factory, extras)[AssistedConsumerViewModel::class]

        assertEquals("assisted", viewModel.argument)
        assertSame(arrangement.consumers.single().observeMessageById, viewModel.message)
        assertSame(arrangement.parent, viewModel.parent)
        owner.viewModelStore.clear()
        assertEquals(1, arrangement.consumers.single().closes)
    }

    @Test
    fun givenManualBinding_whenCreatingThenLookingUpViewModel_thenFactoryIsResolvedOnlyInNewChild() {
        val arrangement = Arrangement()
        val owner = Owner()
        val manualFactory = arrangement.factory.wireManualViewModelFactory(TestManualFactory::class) { extras ->
            create("manual", extras[ARGUMENT])
        }
        assertEquals(0, arrangement.consumers.size)
        val extras = MutableCreationExtras().apply { this[ARGUMENT] = "extras" }
        val provider = ViewModelProvider.create(owner, manualFactory, extras)

        val first = provider["manual-key", ManualConsumerViewModel::class]
        val retained = provider["manual-key", ManualConsumerViewModel::class]

        assertSame(first, retained)
        assertEquals("manual", first.argument)
        assertEquals("extras", first.extra)
        assertSame(arrangement.consumers.single().observeConversationDetails, first.details)
        assertSame(arrangement.consumers.single().observeMessageById, first.message)
        assertEquals(1, arrangement.childGraphs)
        owner.viewModelStore.clear()
        assertEquals(1, arrangement.consumers.single().closes)
    }

    @Test
    fun givenScopedAudioMessageFactory_whenCreatingAndRetainingItems_thenEachNewViewModelUsesItsConsumerObserver() {
        val arrangement = Arrangement()
        val owner = Owner()
        val args = AudioMessageArgs(ConversationId("conversation", "wire.test"), "message")
        // This is the same adapter used by manualScopedViewModelFactory in the Resaca gateway.
        val manualFactory = arrangement.factory.wireManualViewModelFactory(ScopedMessageManualViewModelFactory::class) {
            audioMessageViewModel(args)
        }
        val provider = ViewModelProvider(owner, manualFactory)

        val first = provider["first-item", AudioMessageViewModelImpl::class.java]
        val retained = provider["first-item", AudioMessageViewModelImpl::class.java]
        val second = provider["second-item", AudioMessageViewModelImpl::class.java]

        assertSame(first, retained)
        assertNotSame(first, second)
        assertEquals(2, arrangement.consumers.size)
        arrangement.consumers.forEach { consumer ->
            coVerify(exactly = 1) { consumer.observeMessageById(args.conversationId, args.messageId) }
        }
        owner.viewModelStore.clear()
        arrangement.consumers.forEach { assertEquals(1, it.closes) }
        assertEquals(0, arrangement.parent.closes)
    }

    @Test
    fun givenFailingChildGraph_whenCreatingViewModel_thenConsumerClosesAndParentStaysOpen() {
        val arrangement = Arrangement()
        val failure = IllegalStateException("child graph construction failed")
        arrangement.graphFailure = failure

        val thrown = assertThrows(IllegalStateException::class.java) {
            arrangement.factory.create(DirectConsumerViewModel::class, CreationExtras.Empty)
        }

        assertSame(failure, thrown)
        assertEquals(1, arrangement.consumers.single().closes)
        assertEquals(0, arrangement.parent.closes)
    }

    @Test
    fun givenFailingConstructor_whenCreatingViewModel_thenConsumerCloses() {
        val arrangement = Arrangement()
        assertThrows(IllegalStateException::class.java) {
            arrangement.factory.create(FailingConsumerViewModel::class, CreationExtras.Empty)
        }
        assertEquals(1, arrangement.consumers.single().closes)
    }

    @Test
    fun givenFailingAssistedCreation_whenCreatingViewModel_thenConsumerCloses() {
        val arrangement = Arrangement()
        // The assisted binding requires an argument and fails after its dependencies resolve.
        assertThrows(IllegalStateException::class.java) {
            arrangement.factory.create(AssistedConsumerViewModel::class, CreationExtras.Empty)
        }
        assertEquals(1, arrangement.consumers.single().closes)
    }

    @Test
    fun givenFailingManualCallback_whenCreatingViewModel_thenConsumerCloses() {
        val arrangement = Arrangement()
        val failure = IllegalStateException("manual callback failed")
        val manualFactory = arrangement.factory.wireManualViewModelFactory(TestManualFactory::class) {
            throw failure
        }

        val thrown = assertThrows(IllegalStateException::class.java) {
            manualFactory.create(ManualConsumerViewModel::class, CreationExtras.Empty)
        }

        assertSame(failure, thrown)
        assertEquals(1, arrangement.consumers.single().closes)
        assertEquals(0, arrangement.parent.closes)
    }

    @Test
    fun givenFailingCleanup_whenCreationFails_thenOriginalFailureIsPreserved() {
        val consumer = TestKaliumConsumer()
        val failure = IllegalStateException("creation failed")
        val cleanupFailure = IllegalArgumentException("cleanup failed")
        consumer.closeFailure = cleanupFailure

        val thrown = assertThrows(IllegalStateException::class.java) {
            createConsumerViewModel<ViewModel>({ consumer }) { throw failure }
        }

        assertSame(failure, thrown)
        assertEquals(listOf(cleanupFailure), thrown.suppressed.toList())
        assertEquals(1, consumer.closes)
    }

    private class Owner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    private class Arrangement {
        val parent = CountingResource()
        val hooks = AssemblyHooks()
        val terminalActions = TerminalActions()
        private val audioPlayer = mockk<ConversationAudioMessagePlayer>(relaxed = true) {
            every { observableAudioMessagesState } returns emptyFlow()
            every { audioSpeed } returns emptyFlow()
        }
        val consumers = mutableListOf<TestKaliumConsumer>()
        val replacementConsumers = mutableListOf<TestKaliumConsumer>()
        var childGraphs = 0
        var graphFailure: Throwable? = null
        var graphHook: () -> Unit = {}
        private val parentGraph = createGraphFactory<TestSessionGraph.Factory>().create(parent, audioPlayer, hooks, terminalActions)
        private val userId = UserId("consumer-test", "wire.test")
        private val coreLogic = mockk<CoreLogic>()
        private val session = mockk<UserSessionScope>()
        private val replacementSession = mockk<UserSessionScope>()
        private var sessionActive = true
        private val graphFactory = mockk<AppViewModelGraph.Factory>()
        private val applicationGraph = mockk<WireApplicationGraph>(
            moreInterfaces = arrayOf(AppSessionViewModelGraph.Factory::class),
        )
        val factory: WireSessionMetroViewModelFactory

        init {
            every { applicationGraph.coreLogic } returns coreLogic
            every { coreLogic.getSessionScope(userId) } returns session
            every { session.createConsumer() } answers {
                check(sessionActive) { "The original generation is invalidated" }
                TestKaliumConsumer().also { consumers += it }
            }
            every { replacementSession.createConsumer() } answers {
                TestKaliumConsumer().also { replacementConsumers += it }
            }
            every {
                (applicationGraph as AppSessionViewModelGraph.Factory).createAppSessionViewModelGraph(userId, any())
            } answers {
                val capturedSession = secondArg<UserSessionScope>()
                mockk<AppSessionViewModelGraph> {
                    every { metroViewModelFactory } returns WireSessionMetroViewModelFactory(graphFactory, capturedSession)
                }
            }
            every { graphFactory.createViewModelGraph(any()) } answers {
                childGraphs++
                graphFailure?.let { throw it }
                graphHook()
                val child = parentGraph.childFactory.create(firstArg())
                mockk<AppViewModelGraph> {
                    every { consumerViewModelFactory } returns child.factory
                }
            }
            factory = createSessionFactory()
        }

        fun createSessionFactory(): WireSessionMetroViewModelFactory =
            applicationGraph.createSessionViewModelGraph(userId).metroViewModelFactory as WireSessionMetroViewModelFactory

        fun relogin() {
            sessionActive = false
            consumers.forEach { it.close() }
            every { coreLogic.getSessionScope(userId) } returns replacementSession
        }

        fun verifySessionLookups(count: Int) {
            verify(exactly = count) { coreLogic.getSessionScope(userId) }
        }
    }
}

private val ARGUMENT = object : CreationExtras.Key<String> {}

internal class CountingResource : AutoCloseable {
    var closes = 0
    var closeFailure: Throwable? = null
    override fun close() {
        closes++
        closeFailure?.let { throw it }
    }
}

private class TestKaliumConsumer : KaliumConsumer {
    override val observeConversationDetails = mockk<ObserveConversationDetailsUseCase>()
    override val observeMessageById = mockk<ObserveMessageByIdUseCase>()
    override val getMessageById = mockk<GetMessageByIdUseCase>()
    init {
        coEvery { observeMessageById(any(), any()) } returns emptyFlow()
    }
    private val lock = Any()
    private var open = true
    private val assemblies = mutableSetOf<CompletableDeferred<Unit>>()
    private val jobs = mutableSetOf<Job>()
    var closes = 0
    var closeFailure: Throwable? = null

    override fun <T> assemble(create: () -> T, onDiscard: (T) -> Unit, jobOnDiscard: (T) -> Job?): T {
        val assembly = CompletableDeferred<Unit>()
        synchronized(lock) {
            if (!open) throw CancellationException("Consumer closed")
            assemblies.add(assembly)
        }
        try {
            val value = create()
            val job = jobOnDiscard(value)
            val active = synchronized(lock) {
                open.also { if (!it && job != null) jobs.add(job) }
            }
            if (!active) {
                job?.cancel()
                onDiscard(value)
                throw CancellationException("Consumer invalidated during assembly")
            }
            return value
        } finally {
            assembly.complete(Unit)
            synchronized(lock) { assemblies.remove(assembly) }
        }
    }

    override fun close() {
        val outgoing = synchronized(lock) {
            if (!open) return
            open = false
            closes++
            jobs.toList()
        }
        outgoing.forEach { it.cancel() }
        closeFailure?.let { throw it }
    }

    override suspend fun closeAndJoin() {
        close()
        synchronized(lock) { assemblies.toList() }.forEach { it.await() }
        synchronized(lock) { jobs.toList() }.forEach { it.join() }
    }
}

internal class DirectConsumerViewModel @Inject constructor(
    val details: ObserveConversationDetailsUseCase,
    val message: ObserveMessageByIdUseCase,
    val getMessage: GetMessageByIdUseCase,
    val parent: CountingResource,
) : ViewModel() {
    val ownedResource = CountingResource()
    var cleared = false
    init {
        addCloseable(ownedResource)
    }
    override fun onCleared() { cleared = true }
}

internal class AssemblyHooks {
    var onDirectCreate: (DirectConsumerViewModel) -> Unit = {}
}

internal class TerminalActions {
    val userId = UserId("terminal-caller", "wire.test")
    val currentSession = mockk<CurrentSessionUseCase>()
    val deleteSession = mockk<DeleteSessionUseCase>()
    val switchAccount = mockk<AccountSwitchUseCase>()
    val switchActions = mockk<SwitchAccountActions>(relaxed = true)
    val logout = mockk<LogoutUseCase>()

    init {
        coEvery { currentSession() } returns CurrentSessionResult.Success(AccountInfo.Valid(userId))
        coEvery { switchAccount(SwitchAccountParam.TryToSwitchToNextAccount) } returns SwitchAccountResult.SwitchedToAnotherAccount
    }
}

internal class AssistedConsumerViewModel(
    val argument: String,
    val message: ObserveMessageByIdUseCase,
    val parent: CountingResource,
) : ViewModel()

internal class ManualConsumerViewModel(
    val argument: String,
    val extra: String?,
    val details: ObserveConversationDetailsUseCase,
    val message: ObserveMessageByIdUseCase,
) : ViewModel() {
    var cleared = false
    override fun onCleared() { cleared = true }
}

internal class FailingConsumerViewModel @Inject constructor(val getMessage: GetMessageByIdUseCase) : ViewModel() {
    init {
        error("ViewModel constructor failed")
    }
}

internal class ParentOnlyViewModel : ViewModel()

internal interface TestManualFactory : ManualViewModelAssistedFactory {
    fun create(argument: String, extra: String?): ManualConsumerViewModel
}

@DependencyGraph
internal interface TestSessionGraph : MetroViewModelMultibindings {
    val childFactory: TestConsumerGraph.Factory

    // Like the application graph, the ancestor already has its own ViewModel maps.
    @Provides
    @IntoMap
    @ViewModelKey(ParentOnlyViewModel::class)
    fun parentViewModel(): ViewModel = ParentOnlyViewModel()

    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @Provides parent: CountingResource,
            @Provides audioPlayer: ConversationAudioMessagePlayer,
            @Provides hooks: AssemblyHooks,
            @Provides terminalActions: TerminalActions,
        ): TestSessionGraph
    }
}

@GraphExtension(bindingContainers = [KaliumConsumerModule::class, TestConsumerBindings::class])
internal interface TestConsumerGraph : MetroViewModelMultibindings {
    val factory: WireViewModelGraphFactory

    @GraphExtension.Factory
    interface Factory {
        fun create(@Provides consumer: KaliumConsumer): TestConsumerGraph
    }
}

@BindingContainer
internal object TestConsumerBindings {
    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(SessionAuthenticationManualViewModelFactory::class)
    fun sessionAuthentication(actions: TerminalActions): ManualViewModelAssistedFactory =
        WireMetroViewModelBindings.sessionAuthenticationManualViewModelFactory(
            SessionAuthenticationViewModelFactory(
                currentAccount = actions.userId,
                userDataStoreProvider = mockk(),
                getOrRegisterClient = mockk(),
                isPasswordRequired = mockk(),
                getSelfUser = mockk(),
                requestSecondFactorVerificationCode = mockk(),
                countdownTimer = mockk(),
                fetchSelfClientsFromRemote = mockk(),
                deleteClient = mockk(),
                currentSession = actions.currentSession,
                deleteSession = actions.deleteSession,
                switchAccount = actions.switchAccount,
                logout = actions.logout,
                validateUserHandle = mockk(),
                setUserHandle = mockk(),
                finalizeRegistrationAnalyticsMetadata = mockk(),
                registrationAnalyticsManager = mockk(),
            )
        )

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(ScopedMessageManualViewModelFactory::class)
    fun scopedMessage(audioMessageFactory: AudioMessageViewModelImpl.Factory): ManualViewModelAssistedFactory =
        WireMetroViewModelBindings.scopedMessageManualViewModelFactory(
            compositeMessageFactory = mockk(),
            messageOptionsMenuFactory = mockk(),
            typingIndicatorFactory = mockk(),
            assetLocalPathFactory = mockk(),
            selfDeletingMessageActionFactory = mockk(),
            isFileSharingEnabledProvider = mockk(),
            recordAudioProvider = mockk(),
            audioMessageFactory = audioMessageFactory,
        )

    @Provides
    @IntoMap
    @ViewModelKey(DirectConsumerViewModel::class)
    fun direct(viewModel: DirectConsumerViewModel, hooks: AssemblyHooks): ViewModel = viewModel.also(hooks.onDirectCreate)

    @Provides
    @IntoMap
    @ViewModelKey(FailingConsumerViewModel::class)
    fun failing(viewModel: FailingConsumerViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(AssistedConsumerViewModel::class)
    fun assisted(message: ObserveMessageByIdUseCase, parent: CountingResource): ViewModelAssistedFactory =
        object : ViewModelAssistedFactory {
            override fun create(extras: CreationExtras): ViewModel =
                AssistedConsumerViewModel(checkNotNull(extras[ARGUMENT]), message, parent)
        }

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(TestManualFactory::class)
    fun manual(
        details: ObserveConversationDetailsUseCase,
        message: ObserveMessageByIdUseCase,
    ): ManualViewModelAssistedFactory = object : TestManualFactory {
        override fun create(argument: String, extra: String?): ManualConsumerViewModel =
            ManualConsumerViewModel(argument, extra, details, message)
    }
}
