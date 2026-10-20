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
import io.mockk.every
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class WireSessionMetroViewModelFactoryTest {
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
        val consumer = CountingResource()
        val failure = IllegalStateException("creation failed")
        val cleanupFailure = IllegalArgumentException("cleanup failed")
        consumer.closeFailure = cleanupFailure

        val thrown = assertThrows(IllegalStateException::class.java) {
            createConsumerViewModel<CountingResource, ViewModel>({ consumer }) { throw failure }
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
        private val audioPlayer = mockk<ConversationAudioMessagePlayer>(relaxed = true) {
            every { observableAudioMessagesState } returns emptyFlow()
            every { audioSpeed } returns emptyFlow()
        }
        val consumers = mutableListOf<TestKaliumConsumer>()
        var childGraphs = 0
        var graphFailure: Throwable? = null
        private val parentGraph = createGraphFactory<TestSessionGraph.Factory>().create(parent, audioPlayer)
        private val userId = UserId("consumer-test", "wire.test")
        private val coreLogic = mockk<CoreLogic>()
        private val session = mockk<UserSessionScope>()
        private val graphFactory = mockk<AppViewModelGraph.Factory>()
        val factory = WireSessionMetroViewModelFactory(graphFactory, coreLogic, userId)

        init {
            every { coreLogic.getSessionScope(userId) } returns session
            every { session.createConsumer() } answers {
                TestKaliumConsumer().also { consumers += it }
            }
            every { graphFactory.createViewModelGraph(any()) } answers {
                childGraphs++
                graphFailure?.let { throw it }
                val child = parentGraph.childFactory.create(firstArg())
                mockk<AppViewModelGraph> {
                    every { consumerViewModelFactory } returns child.factory
                }
            }
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
    var closes = 0
    override fun close() { closes++ }
}

internal class DirectConsumerViewModel @Inject constructor(
    val details: ObserveConversationDetailsUseCase,
    val message: ObserveMessageByIdUseCase,
    val getMessage: GetMessageByIdUseCase,
    val parent: CountingResource,
) : ViewModel()

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
) : ViewModel()

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
    fun direct(viewModel: DirectConsumerViewModel): ViewModel = viewModel

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
