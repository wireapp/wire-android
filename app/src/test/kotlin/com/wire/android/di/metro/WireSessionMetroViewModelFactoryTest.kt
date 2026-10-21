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
import com.wire.android.media.audiomessage.AudioMessageArgs
import com.wire.android.media.audiomessage.AudioMessageViewModelImpl
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.ui.home.conversations.ScopedMessageManualViewModelFactory
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.di.KaliumConsumer
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageByIdUseCase
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun givenSameOwnerAndKey_whenLookingUpAgain_thenConsumerAndChildGraphAreRetained() {
        val arrangement = Arrangement()
        val owner = ReadOwner()
        assertTrue(arrangement.consumers.isEmpty())
        val first = ViewModelProvider(owner, arrangement.factory)["same", DirectReadViewModel::class.java]
        val retained = ViewModelProvider(owner, arrangement.factory)["same", DirectReadViewModel::class.java]

        assertSame(first, retained)
        assertEquals(1, arrangement.childGraphs)
        assertEquals(1, arrangement.consumers.size)
        owner.viewModelStore.clear()
        verify(exactly = 1) { arrangement.consumers.single().close() }
    }

    @Test
    fun givenDifferentOwnersOrKeys_whenCreating_thenChildReadsDifferAndParentStaysShared() {
        val arrangement = Arrangement()
        val owner = ReadOwner()
        val otherOwner = ReadOwner()
        val provider = ViewModelProvider(owner, arrangement.factory)
        val first = provider["first", DirectReadViewModel::class.java]
        val second = provider["second", DirectReadViewModel::class.java]
        val third = ViewModelProvider(otherOwner, arrangement.factory)["first", DirectReadViewModel::class.java]

        listOf(first, second, third).forEachIndexed { index, vm ->
            val consumer = arrangement.consumers[index]
            assertSame(consumer.observeConversationDetails, vm.details)
            assertSame(consumer.observeMessageById, vm.message)
            assertSame(consumer.getMessageById, vm.getMessage)
            assertSame(arrangement.parent, vm.parent)
        }
        assertNotSame(first.details, second.details)
        assertNotSame(first.message, third.message)
        assertNotSame(second.getMessage, third.getMessage)
        owner.viewModelStore.clear()
        owner.viewModelStore.clear()
        verify(exactly = 1) { arrangement.consumers[0].close(); arrangement.consumers[1].close() }
        verify(exactly = 0) { arrangement.consumers[2].close(); arrangement.parent.close() }
        otherOwner.viewModelStore.clear()
        verify(exactly = 1) { arrangement.consumers[2].close() }
    }

    @Test
    fun givenAssistedBinding_whenCreating_thenChildReadsAndCreationExtrasAreUsed() {
        val arrangement = Arrangement()
        val owner = ReadOwner()
        val extras = MutableCreationExtras().apply { this[READ_ARGUMENT] = "assisted" }
        val vm = ViewModelProvider.create(owner, arrangement.factory, extras)[AssistedReadViewModel::class]

        assertEquals("assisted", vm.argument)
        assertSame(arrangement.consumers.single().observeMessageById, vm.message)
        owner.viewModelStore.clear()
        verify(exactly = 1) { arrangement.consumers.single().close() }
    }

    @Test
    fun givenManualBinding_whenCreatingAndRetaining_thenFactoryAndCallbackRunOnlyForNewViewModel() {
        val arrangement = Arrangement()
        val owner = ReadOwner()
        var callbacks = 0
        val factory = arrangement.factory.wireManualViewModelFactory(ReadManualFactory::class) { extras ->
            callbacks++
            create(checkNotNull(extras[READ_ARGUMENT]))
        }
        assertTrue(arrangement.consumers.isEmpty())
        val extras = MutableCreationExtras().apply { this[READ_ARGUMENT] = "manual" }
        val provider = ViewModelProvider.create(owner, factory, extras)
        val first = provider["manual", AssistedReadViewModel::class]

        assertSame(first, provider["manual", AssistedReadViewModel::class])
        assertEquals("manual", first.argument)
        assertSame(arrangement.consumers.single().observeMessageById, first.message)
        assertEquals(1, callbacks)
        assertEquals(1, arrangement.childGraphs)
        owner.viewModelStore.clear()
        verify(exactly = 1) { arrangement.consumers.single().close() }
    }

    @Test
    fun givenNonConsumerFactory_whenUsingSharedManualAdapter_thenExistingFactoryAndExtrasArePreserved() {
        val message = mockk<ObserveMessageByIdUseCase>()
        val manual = object : ReadManualFactory {
            override fun create(argument: String): AssistedReadViewModel = AssistedReadViewModel(argument, message)
        }
        val metro = WireViewModelGraphFactory(emptyMap(), emptyMap(), mapOf(ReadManualFactory::class to { manual }))
        val factory = metro.wireManualViewModelFactory(ReadManualFactory::class) { extras ->
            create(checkNotNull(extras[READ_ARGUMENT]))
        }
        val extras = MutableCreationExtras().apply { this[READ_ARGUMENT] = "authentication" }
        val owner = ReadOwner()
        val vm = ViewModelProvider.create(owner, factory, extras)[AssistedReadViewModel::class]
        assertEquals("authentication", vm.argument)
        assertSame(message, vm.message)
        owner.viewModelStore.clear()
    }

    @Test
    fun givenActualScopedAudioViewModel_whenCreatingAndRetaining_thenEachNewItemUsesItsConsumer() {
        val arrangement = Arrangement()
        val owner = ReadOwner()
        val args = AudioMessageArgs(ConversationId("conversation", "wire.test"), "message")
        // Both the navigation and Resaca gateways use this adapter.
        val factory = arrangement.factory.wireManualViewModelFactory(ScopedMessageManualViewModelFactory::class) {
            audioMessageViewModel(args)
        }
        val provider = ViewModelProvider(owner, factory)
        val first = provider["first", AudioMessageViewModelImpl::class.java]
        assertSame(first, provider["first", AudioMessageViewModelImpl::class.java])
        assertNotSame(first, provider["second", AudioMessageViewModelImpl::class.java])
        assertEquals(2, arrangement.consumers.size)
        arrangement.consumers.forEach { consumer ->
            coVerify(exactly = 1) { consumer.observeMessageById(args.conversationId, args.messageId) }
        }
        owner.viewModelStore.clear()
        arrangement.consumers.forEach { consumer -> verify(exactly = 1) { consumer.close() } }
        verify(exactly = 0) { arrangement.parent.close() }
    }

    @Test
    fun givenSameAccountReplacement_whenUsingRetainedFactory_thenItKeepsTheCapturedGeneration() {
        val arrangement = Arrangement()
        val owner = ReadOwner()
        val provider = ViewModelProvider(owner, arrangement.factory)
        val retained = provider["retained", DirectReadViewModel::class.java]
        every { arrangement.session.createConsumer() } throws IllegalStateException("Outgoing generation")
        every { arrangement.coreLogic.getSessionScope(arrangement.userId) } returns mockk()

        assertSame(retained, provider["retained", DirectReadViewModel::class.java])
        assertThrows(IllegalStateException::class.java) { provider["new", DirectReadViewModel::class.java] }
        verify(exactly = 1) { arrangement.coreLogic.getSessionScope(arrangement.userId) }
        assertEquals(1, arrangement.childGraphs)
        owner.viewModelStore.clear()
    }

    @Test
    fun givenFailingCreation_whenCreating_thenOnlyThatConsumerCloses() {
        val arrangement = Arrangement()
        val owner = ReadOwner()
        val sibling = ViewModelProvider(owner, arrangement.factory)[DirectReadViewModel::class.java]
        val failure = IllegalStateException("Graph creation failed")
        arrangement.graphFailure = failure
        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            arrangement.factory.create(DirectReadViewModel::class, CreationExtras.Empty)
        })
        arrangement.graphFailure = null
        assertThrows(IllegalStateException::class.java) {
            arrangement.factory.create(FailingReadViewModel::class, CreationExtras.Empty)
        }
        assertThrows(IllegalStateException::class.java) {
            arrangement.factory.create(AssistedReadViewModel::class, CreationExtras.Empty)
        }
        val manual = arrangement.factory.wireManualViewModelFactory(ReadManualFactory::class) { throw failure }
        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            manual.create(AssistedReadViewModel::class, CreationExtras.Empty)
        })
        assertEquals(5, arrangement.consumers.size)
        arrangement.consumers.drop(1).forEach { consumer -> verify(exactly = 1) { consumer.close() } }
        assertTrue(sibling.viewModelScope.coroutineContext[Job]!!.isActive)
        verify(exactly = 0) { arrangement.consumers.first().close(); arrangement.parent.close() }
        owner.viewModelStore.clear()
    }

    @Test
    fun givenDiscardedAssembly_whenSdkRejectsPublication_thenViewModelIsClearedAndItsJobIsTheDrainJob() {
        val consumer = readConsumer()
        val vm = DirectReadViewModel(mockk(), mockk(), mockk(), mockk())
        val owned = mockk<AutoCloseable>(relaxed = true)
        vm.addCloseable(owned)
        every { consumer.assemble<ViewModel>(any(), any(), any()) } answers {
            val assembled = firstArg<() -> ViewModel>()()
            assertSame(vm.viewModelScope.coroutineContext[Job], thirdArg<(ViewModel) -> Job?>()(assembled))
            secondArg<(ViewModel) -> Unit>()(assembled)
            throw CancellationException("Invalidated during assembly")
        }

        assertThrows(CancellationException::class.java) { createConsumerViewModel({ consumer }) { vm } }

        assertTrue(vm.cleared)
        assertTrue(vm.viewModelScope.coroutineContext[Job]!!.isCancelled)
        verify(exactly = 1) { consumer.close(); owned.close() }
        verify(exactly = 0) { vm.parent.close() }
    }

    @Test
    fun givenFailingCleanup_whenCreationFails_thenOriginalFailureIsPreserved() {
        val consumer = readConsumer()
        val failure = IllegalStateException("Creation failed")
        val cleanupFailure = IllegalArgumentException("Cleanup failed")
        every { consumer.close() } throws cleanupFailure

        val thrown = assertThrows(IllegalStateException::class.java) {
            createConsumerViewModel<ViewModel>({ consumer }) { throw failure }
        }

        assertSame(failure, thrown)
        assertEquals(listOf(cleanupFailure), thrown.suppressed.toList())
        verify(exactly = 1) { consumer.close() }
    }

    private class Arrangement {
        val parent = mockk<AutoCloseable>(relaxed = true)
        val consumers = mutableListOf<KaliumConsumer>()
        val userId = UserId("consumer-test", "wire.test")
        val session = mockk<UserSessionScope>()
        val coreLogic = mockk<CoreLogic>()
        var childGraphs = 0
        var graphFailure: Throwable? = null
        private val player = mockk<ConversationAudioMessagePlayer>(relaxed = true) {
            every { observableAudioMessagesState } returns emptyFlow()
            every { audioSpeed } returns emptyFlow()
        }
        private val parentGraph = createGraphFactory<ReadSessionGraph.Factory>().create(parent, player)
        private val graphFactory = mockk<AppViewModelGraph.Factory>()
        val factory: WireSessionMetroViewModelFactory

        init {
            every { session.createConsumer() } answers { readConsumer().also(consumers::add) }
            every { coreLogic.getSessionScope(userId) } returns session
            val application = mockk<WireApplicationGraph>(moreInterfaces = arrayOf(AppSessionViewModelGraph.Factory::class))
            every { application.coreLogic } returns coreLogic
            every {
                (application as AppSessionViewModelGraph.Factory).createAppSessionViewModelGraph(userId, any())
            } answers {
                val captured = secondArg<UserSessionScope>()
                mockk<AppSessionViewModelGraph> {
                    every { metroViewModelFactory } returns WireSessionMetroViewModelFactory(graphFactory, captured)
                }
            }
            every { graphFactory.createViewModelGraph(any()) } answers {
                childGraphs++
                graphFailure?.let { throw it }
                val child = parentGraph.childFactory.create(firstArg())
                mockk<AppViewModelGraph> { every { consumerViewModelFactory } returns child.factory }
            }
            factory = application.createSessionViewModelGraph(userId).metroViewModelFactory as WireSessionMetroViewModelFactory
        }
    }
}

// Assembly semantics are tested with the real SDK in WireKaliumConsumerIntegrationTest. This mock
// only runs successful assembly, keeping these tests focused on Android factory/owner behavior.
private fun readConsumer(): KaliumConsumer {
    val message = mockk<ObserveMessageByIdUseCase>()
    coEvery { message(any(), any()) } returns emptyFlow()
    return mockk {
        every { observeConversationDetails } returns mockk()
        every { observeMessageById } returns message
        every { getMessageById } returns mockk()
        every { close() } returns Unit
        every { assemble<ViewModel>(any(), any(), any()) } answers { firstArg<() -> ViewModel>()() }
    }
}

private val READ_ARGUMENT = object : CreationExtras.Key<String> {}

private class ReadOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

internal class DirectReadViewModel @Inject constructor(
    val details: ObserveConversationDetailsUseCase,
    val message: ObserveMessageByIdUseCase,
    val getMessage: GetMessageByIdUseCase,
    val parent: AutoCloseable,
) : ViewModel() {
    var cleared = false
    override fun onCleared() { cleared = true }
}

internal class AssistedReadViewModel(val argument: String, val message: ObserveMessageByIdUseCase) : ViewModel()

internal class FailingReadViewModel @Inject constructor(val message: ObserveMessageByIdUseCase) : ViewModel() {
    init { error("ViewModel constructor failed") }
}

internal interface ReadManualFactory : ManualViewModelAssistedFactory {
    fun create(argument: String): AssistedReadViewModel
}

@DependencyGraph
internal interface ReadSessionGraph : MetroViewModelMultibindings {
    val childFactory: ReadConsumerGraph.Factory

    // The application ancestor also has unrelated ViewModel maps.
    @Provides
    @IntoMap
    @ViewModelKey(AncestorReadViewModel::class)
    fun ancestorViewModel(): ViewModel = AncestorReadViewModel()

    @DependencyGraph.Factory
    interface Factory {
        fun create(@Provides parent: AutoCloseable, @Provides player: ConversationAudioMessagePlayer): ReadSessionGraph
    }
}

internal class AncestorReadViewModel : ViewModel()

@GraphExtension(bindingContainers = [KaliumConsumerModule::class, ReadConsumerBindings::class])
internal interface ReadConsumerGraph : MetroViewModelMultibindings {
    val factory: WireViewModelGraphFactory

    @GraphExtension.Factory
    interface Factory {
        fun create(@Provides consumer: KaliumConsumer): ReadConsumerGraph
    }
}

@BindingContainer
internal object ReadConsumerBindings {
    @Provides
    @IntoMap
    @ViewModelKey(DirectReadViewModel::class)
    fun direct(vm: DirectReadViewModel): ViewModel = vm

    @Provides
    @IntoMap
    @ViewModelKey(FailingReadViewModel::class)
    fun failing(vm: FailingReadViewModel): ViewModel = vm

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(AssistedReadViewModel::class)
    fun assisted(message: ObserveMessageByIdUseCase): ViewModelAssistedFactory = object : ViewModelAssistedFactory {
        override fun create(extras: CreationExtras): ViewModel = AssistedReadViewModel(checkNotNull(extras[READ_ARGUMENT]), message)
    }

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(ReadManualFactory::class)
    fun manual(message: ObserveMessageByIdUseCase): ManualViewModelAssistedFactory = object : ReadManualFactory {
        override fun create(argument: String): AssistedReadViewModel = AssistedReadViewModel(argument, message)
    }

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(ScopedMessageManualViewModelFactory::class)
    fun audio(factory: AudioMessageViewModelImpl.Factory): ManualViewModelAssistedFactory =
        WireMetroViewModelBindings.scopedMessageManualViewModelFactory(
            compositeMessageFactory = mockk(), messageOptionsMenuFactory = mockk(), typingIndicatorFactory = mockk(),
            assetLocalPathFactory = mockk(), selfDeletingMessageActionFactory = mockk(),
            isFileSharingEnabledProvider = mockk(), recordAudioProvider = mockk(), audioMessageFactory = factory,
        )
}
