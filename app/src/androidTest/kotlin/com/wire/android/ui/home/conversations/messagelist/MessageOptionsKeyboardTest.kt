/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.conversations.messagelist

import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.media.audiomessage.PlayingAudioMessage
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.home.conversations.MessageOptionsState
import com.wire.android.ui.home.conversations.rememberMessageOptionsState
import com.wire.android.ui.home.conversations.edit.MessageOptionsMenuState
import com.wire.android.ui.home.conversations.edit.MessageOptionsMenuViewModel
import com.wire.android.ui.home.conversations.edit.MessageOptionsModalSheetLayout
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.item.MessageClickActions
import com.wire.android.ui.home.conversations.mock.mockFooter
import com.wire.android.ui.home.conversations.mock.mockMessageWithKnock
import com.wire.android.ui.home.conversations.mock.mockMessageWithTextContent
import com.wire.android.ui.home.conversations.mock.mockedImageUIMessage
import com.wire.android.ui.home.conversations.model.MessageSenderId
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.handleContextMenuShortcut
import com.wire.kalium.logic.data.conversation.InteractionAvailability
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import android.view.KeyEvent as AndroidKeyEvent

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class MessageOptionsKeyboardTest(private val bubbles: Boolean, private val own: Boolean) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "bubbles={0}, own={1}")
        fun parameters() = listOf(arrayOf(false, false), arrayOf(false, true), arrayOf(true, false), arrayOf(true, true))
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val stateRestoration = StateRestorationTester(rule)
    private var hostFocusOverride: MutableState<Boolean>? = null
    private val opened = mutableListOf<String>()
    private val reactions = mutableListOf<String>()
    private val links = mutableListOf<String>()
    private val profiles = mutableListOf<MessageSenderId>()
    private val images = mutableListOf<String>()
    private lateinit var sheet: WireModalSheetState<String>
    private lateinit var menuState: MutableStateFlow<MessageOptionsMenuState>
    private lateinit var pages: MutableStateFlow<PagingData<UIMessage>>
    private lateinit var optionsState: MessageOptionsState
    private var lastOpeningJob: Job? = null

    @Test
    fun shortcutsOpenExistingSheetAndHandleDismissalRestoresMessage() {
        showMessages()
        listOf(false, true).forEach { shifted ->
            focusMessage()
            shortcut(shifted)
            rule.onNodeWithText("Reply").assertIsDisplayed()
            assertEquals("target", opened.last())
            focusHandle()
            key(Key.Tab)
            rule.onNodeWithText("👍").assertIsFocused()
            key(Key.Tab, shifted = true)
            rule.onNodeWithContentDescription("Drag handle").assertIsFocused()
            key(if (shifted) Key.Spacebar else Key.Enter)
            rule.onNodeWithTag("message_target").assertIsFocused()
        }
        assertEquals(2, opened.size)
    }

    @Test
    fun touchLongPressStillReturnsFocusToComposerOnDismissal() {
        showMessages()
        rule.onNodeWithText("Keyboard message").performTouchInput { longClick() }
        rule.onNodeWithText("Reply").assertIsDisplayed()
        dismissSheet()
        rule.onNodeWithTag("composer").assertIsFocused()
        assertFalse(optionsState.retainMessageNavigation)
    }

    @Test
    fun nativeBackDismissalRestoresInvokingMessage() {
        showMessages()
        focusMessage()
        shortcut(shifted = true)
        focusHandle()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        rule.waitUntil(5_000) { !sheet.isVisible && !optionsState.retainMessageNavigation }
        rule.onNodeWithTag("message_target").assertIsFocused()
    }

    @Test
    fun unavailableMessageClosesOptionsAndRestoresList() {
        showMessages()
        focusMessage()
        shortcut(shifted = false)
        rule.runOnIdle {
            pages.value = PagingData.from(listOf(message("replacement")))
            menuState.value = MessageOptionsMenuState.NotAvailable
        }
        rule.waitUntil(5_000) { !sheet.isVisible && !optionsState.retainMessageNavigation }
        rule.onNodeWithContentDescription("Conversation messages.", substring = true).assertIsFocused()
    }

    @Test
    fun nestedUrlOpensOwningMessageMenuWithoutActivatingUrl() {
        showMessages(text = "https://wire.com")
        focusMessage()
        focusInline("https://wire.com")
        shortcut(shifted = true)
        rule.onNodeWithText("Reply").assertIsDisplayed()
        assertEquals(listOf("target"), opened)
        assertTrue(links.isEmpty())
        dismissSheet()
        focusInline("https://wire.com")
        key(Key.Enter)
        assertEquals(listOf("https://wire.com"), links)
    }

    @Test
    fun nestedMentionOpensMenuAndRetainsNormalActivation() {
        showMessages(mention = true)
        focusMessage()
        focusInline("@Heidi")
        shortcut(shifted = false)
        assertEquals(listOf("target"), opened)
        assertTrue(profiles.isEmpty())
        dismissSheet()
        focusInline("@Heidi")
        key(Key.Spacebar)
        assertEquals(1, profiles.size)
    }

    @Test
    fun nestedReactionOpensOwningMessageMenu() {
        showMessages(withReaction = true)
        focusMessage()
        rule.onNode(hasText("👍", substring = true) and hasClickAction())
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        shortcut(shifted = false)
        assertEquals(listOf("target"), opened)
        rule.onNodeWithText("Reply").assertIsDisplayed()
    }

    @Test
    fun imageMessageSupportsMenuFromNestedImageTarget() {
        showMessages(image = true)
        focusMessage()
        val imageTarget = rule.onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag("message_target"))).onLast()
        imageTarget.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        shortcut(shifted = true)
        assertEquals(listOf("target"), opened)
        assertTrue(images.isEmpty())
        rule.onNodeWithText("Reply").assertIsDisplayed()
        dismissSheet()
        imageTarget.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        key(Key.Enter)
        assertEquals(listOf("target"), images)
    }

    @Test
    fun replyHandsFocusToComposer() {
        showMessages()
        focusMessage()
        shortcut(shifted = false)
        activateMenuItem("Reply")
        rule.onNodeWithTag("composer").assertIsFocused()
        rule.runOnIdle { assertFalse(optionsState.retainMessageNavigation) }
    }

    @Test
    fun editHandsFocusToComposerOrRemainsUnavailableForOthers() {
        showMessages()
        focusMessage()
        shortcut(shifted = false)
        if (own) {
            activateMenuItem("Edit text")
            rule.onNodeWithTag("composer").assertIsFocused()
        } else {
            rule.onNodeWithText("Edit text").assertDoesNotExist()
        }
    }

    @Test
    fun reactionSelectionUsesExistingCallbackAndRestoresMessage() {
        showMessages()
        focusMessage()
        shortcut(shifted = false)
        rule.onNodeWithText("👍").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        key(Key.Spacebar)
        rule.onNodeWithTag("message_target").assertIsFocused()
        assertEquals(listOf("👍"), reactions)
    }

    @Test
    fun removedMessageFallsBackToListAndCanEnterNavigationAgain() {
        showMessages()
        focusMessage()
        shortcut(shifted = true)
        rule.runOnIdle { pages.value = PagingData.from(listOf(message("replacement"))) }
        rule.waitForIdle()
        dismissSheet()
        rule.onNodeWithContentDescription("Conversation messages.", substring = true).assertIsFocused()
        key(Key.Enter)
        rule.onNodeWithTag("message_replacement").assertIsFocused()
    }

    @Test
    fun deletedMessageDoesNotOpenOptions() {
        showMessages(deleted = true)
        focusMessage()
        shortcut(shifted = false)
        shortcut(shifted = true)
        assertTrue(opened.isEmpty())
    }

    @Test
    fun systemMessageDoesNotOpenOptions() {
        showMessages(system = true)
        focusMessage()
        shortcut(shifted = false)
        shortcut(shifted = true)
        assertTrue(opened.isEmpty())
    }

    @Test
    fun messageWithoutOptionsCallbackDoesNotConsumeShortcut() {
        showMessages(optionsAvailable = false)
        focusMessage()
        shortcut(shifted = true)
        rule.onNodeWithTag("message_target").assertIsFocused()
        assertTrue(opened.isEmpty())
    }

    @Test
    fun olderMessageOpensItsOwnOptions() {
        showMessages(secondMessage = true)
        focusMessage()
        rule.onNodeWithTag("message_older").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        shortcut(shifted = true)
        assertEquals(listOf("older"), opened)
        dismissSheet()
        rule.onNodeWithTag("message_older").assertIsFocused()
    }

    @Test
    fun modifierVariantsAndRepeatsDoNotInvokeOptions() {
        var count = 0
        val modifiers = listOf(AndroidKeyEvent.META_CTRL_ON, AndroidKeyEvent.META_ALT_ON, AndroidKeyEvent.META_META_ON)
        modifiers.forEach { modifier ->
            val event = nativeEvent(AndroidKeyEvent.KEYCODE_F10, AndroidKeyEvent.META_SHIFT_ON or modifier)
            assertFalse(event.handleContextMenuShortcut { count++ })
        }
        assertFalse(nativeEvent(AndroidKeyEvent.KEYCODE_F10).handleContextMenuShortcut { count++ })
        assertFalse(
            nativeEvent(AndroidKeyEvent.KEYCODE_MENU, AndroidKeyEvent.META_SHIFT_ON).handleContextMenuShortcut {
                count++
            }
        )
        assertTrue(nativeEvent(AndroidKeyEvent.KEYCODE_MENU, repeat = 1).handleContextMenuShortcut { count++ })
        assertTrue(
            nativeEvent(AndroidKeyEvent.KEYCODE_F10, AndroidKeyEvent.META_SHIFT_ON).handleContextMenuShortcut {
                count++
            }
        )
        assertEquals(1, count)
    }

    @Test
    fun moreEmojisCanBeCancelledBackToOptions() {
        showMessages()
        focusMessage()
        shortcut(shifted = false)
        openEmojiPicker()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_BACK)
        rule.waitForIdle()
        rule.onNodeWithText("Reply").assertIsDisplayed()
        rule.onNodeWithContentDescription("More emojis").assertIsFocused()
        dismissSheet()
        rule.onNodeWithTag("message_target").assertIsFocused()
    }

    @Test
    @androidx.test.filters.SdkSuppress(minSdkVersion = 29)
    fun moreEmojisSupportsNativeKeyboardSelection() {
        showMessages()
        focusMessage()
        shortcut(shifted = false)
        openEmojiPicker()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var emojiFocused = false
        var remainingTabs = 50
        while (remainingTabs > 0) {
            rule.runOnIdle {
                emojiFocused = WindowInspector.getGlobalWindowViews()
                    .mapNotNull { it.findFocus() }
                    .any { it.javaClass.simpleName == "EmojiView" }
            }
            if (emojiFocused) break
            instrumentation.sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_TAB)
            rule.waitForIdle()
            remainingTabs--
        }
        assertTrue("Tab must reach a native emoji cell", emojiFocused)
        instrumentation.sendKeyDownUpSync(AndroidKeyEvent.KEYCODE_ENTER)
        rule.waitUntil(5_000) { !sheet.isVisible && !optionsState.retainMessageNavigation }
        rule.waitForIdle()
        assertEquals(1, reactions.size)
        rule.onNodeWithTag("message_target").assertIsFocused()
    }

    @Test
    fun standaloneListAllowsBothShortcutsWithoutGettingStuck() {
        showMessages(withOwner = false, noShow = true)
        focusMessage()
        shortcut(shifted = false)
        shortcut(shifted = true)
        assertEquals(listOf("target", "target"), opened)
        rule.onNodeWithTag("message_target").assertIsFocused()
    }

    @Test
    fun callbackWithoutOpeningRestoresFocusAndAllowsBothShortcuts() {
        showMessages(noShow = true)
        focusMessage()
        shortcut(shifted = false)
        rule.waitUntil { !optionsState.retainMessageNavigation }
        shortcut(shifted = true)
        rule.waitUntil { !optionsState.retainMessageNavigation }
        assertEquals(listOf("target", "target"), opened)
        rule.onNodeWithTag("message_target").assertIsFocused()
    }

    @Test
    fun cancelledOpeningRestoresMessageAndAllowsTheOtherShortcut() {
        val scheduler = TestCoroutineScheduler()
        showMessages(openingScheduler = scheduler)
        focusMessage()
        shortcut(shifted = false)
        rule.runOnIdle {
            scheduler.runCurrent()
            scheduler.advanceTimeBy(100)
            lastOpeningJob!!.cancel()
            scheduler.runCurrent()
        }
        rule.waitUntil { !optionsState.retainMessageNavigation }
        rule.onNodeWithTag("message_target").assertIsFocused()
        shortcut(shifted = true)
        rule.runOnIdle { scheduler.advanceUntilIdle() }
        rule.waitForIdle()
        rule.onNodeWithText("Reply").assertIsDisplayed()
        assertEquals(listOf("target", "target"), opened)
    }

    @Test
    fun recreationDuringOpeningDiscardsRequestAndAllowsReopening() {
        val scheduler = TestCoroutineScheduler()
        showMessages(openingScheduler = scheduler)
        focusMessage()
        shortcut(shifted = false)
        rule.runOnIdle {
            scheduler.runCurrent()
            scheduler.advanceTimeBy(100)
            assertTrue(optionsState.isOpening)
            assertFalse(sheet.isVisible)
        }
        val interruptedJob = lastOpeningJob!!
        stateRestoration.emulateSavedInstanceStateRestore()
        rule.runOnIdle {
            scheduler.runCurrent()
            assertTrue(interruptedJob.isCancelled)
            assertFalse(optionsState.retainMessageNavigation)
            assertFalse(sheet.isVisible)
        }
        focusMessage()
        shortcut(shifted = true)
        rule.runOnIdle { scheduler.advanceUntilIdle() }
        rule.waitForIdle()
        rule.onNodeWithText("Reply").assertIsDisplayed()
        assertEquals(listOf("target", "target"), opened)
    }

    @Test
    fun recreationWithVisibleSheetPreservesMessageRestoration() {
        showMessages()
        focusMessage()
        shortcut(shifted = true)
        stateRestoration.emulateSavedInstanceStateRestore()
        rule.runOnIdle { assertTrue(optionsState.retainMessageNavigation) }
        rule.onNodeWithText("Reply").assertIsDisplayed()
        dismissSheet()
        rule.onNodeWithTag("message_target").assertIsFocused()
    }

    @Test
    fun recreationAfterDismissalWaitsForWindowFocusThenRestoresMessage() {
        val windowFocused = mutableStateOf(true)
        showMessages(windowFocused = windowFocused)
        focusMessage()
        shortcut(shifted = false)
        rule.runOnIdle { windowFocused.value = false }
        dismissSheet()
        rule.runOnIdle {
            assertFalse(sheet.isVisible)
            assertTrue(optionsState.retainMessageNavigation)
        }
        stateRestoration.emulateSavedInstanceStateRestore()
        rule.runOnIdle { assertTrue(optionsState.retainMessageNavigation) }
        rule.runOnIdle { windowFocused.value = true }
        rule.waitUntil { !optionsState.retainMessageNavigation }
        rule.onNodeWithTag("message_target").assertIsFocused()
    }

    @Test
    fun recreationWithRemovedMessageRestoresList() {
        showMessages()
        focusMessage()
        shortcut(shifted = false)
        rule.runOnIdle { pages.value = PagingData.from(listOf(message("replacement"))) }
        rule.waitForIdle()
        stateRestoration.emulateSavedInstanceStateRestore()
        dismissSheet()
        rule.onNodeWithContentDescription("Conversation messages.", substring = true).assertIsFocused()
    }

    private fun openEmojiPicker() {
        rule.onNodeWithContentDescription("More emojis").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        key(Key.Enter)
        rule.onNodeWithText("Select Reaction").assertIsDisplayed()
    }

    private fun message(id: String = "target", text: String = "Keyboard message") = mockMessageWithTextContent(text).let {
        it.copy(header = it.header.copy(messageId = id), source = if (own) MessageSource.Self else MessageSource.OtherUser)
    }

    // Keep the real list/sheet fixture shared by all rendering, message and lifecycle cases.
    @Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
    private fun showMessages(
        text: String = "Keyboard message",
        withReaction: Boolean = false,
        image: Boolean = false,
        deleted: Boolean = false,
        system: Boolean = false,
        optionsAvailable: Boolean = true,
        secondMessage: Boolean = false,
        mention: Boolean = false,
        withOwner: Boolean = true,
        noShow: Boolean = false,
        openingScheduler: TestCoroutineScheduler? = null,
        windowFocused: MutableState<Boolean>? = null,
    ) {
        hostFocusOverride = windowFocused
        val regular = (if (image) mockedImageUIMessage() else message(text = text)).let {
            it.copy(
                source = if (own) MessageSource.Self else MessageSource.OtherUser,
                header = it.header.copy(messageId = "target", messageStatus = it.header.messageStatus.copy(isDeleted = deleted)),
                messageFooter = if (withReaction) mockFooter else it.messageFooter,
                messageContent = if (mention) {
                    val content = it.messageContent as UIMessageContent.TextMessage
                    val mentions = listOf(MessageMention(0, 6, UserId("heidi", "wire.com"), false))
                    val mentionText = UIText.DynamicString("@Heidi", mentions = mentions)
                    content.copy(messageBody = content.messageBody.copy(message = mentionText))
                } else {
                    it.messageContent
                },
            )
        }
        val target: UIMessage = if (system) mockMessageWithKnock.copy(header = regular.header) else regular
        pages = MutableStateFlow(PagingData.from(if (secondMessage) listOf(target, message("older")) else listOf(target)))
        menuState = MutableStateFlow<MessageOptionsMenuState>(MessageOptionsMenuState.Message(regular))
        val viewModel = object : MessageOptionsMenuViewModel {
            override fun observeMessageStateFlow(messageId: String): StateFlow<MessageOptionsMenuState> = menuState
        }
        stateRestoration.setContent {
            WireTestTheme {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    val composerFocus = remember { FocusRequester() }
                    optionsState = rememberTestOptionsState(openingScheduler) { composerFocus.requestFocus() }
                    sheet = optionsState.sheetState
                    val inputMode = LocalInputModeManager.current
                    LaunchedEffect(Unit) { inputMode.requestInputMode(InputMode.Keyboard) }
                    val content: @Composable () -> Unit = {
                        Column(Modifier.fillMaxSize()) {
                            ConversationMessageList(
                                lazyPagingMessages = pages.collectAsLazyPagingItems(),
                                lazyListState = rememberLazyListState(),
                                lastUnreadMessageInstant = null,
                                adminlessGroupDeletionTimestamp = null,
                                playingAudioMessage = PlayingAudioMessage.None,
                                assetStatuses = persistentMapOf(),
                                onUpdateConversationReadDate = {},
                                onSwipedToReply = {},
                                onSwipedToReact = {},
                                onSelfDeletingMessageRead = {},
                                conversationDetailsData = ConversationDetailsData.None(null),
                                selectedMessageId = null,
                                interactionAvailability = InteractionAvailability.ENABLED,
                                clickActions = MessageClickActions.Content(
                                    onFullMessageLongClicked = if (optionsAvailable) {
                                        {
                                            opened.add(it.header.messageId)
                                            if (!noShow) lastOpeningJob = optionsState.open(it.header.messageId)
                                        }
                                    } else {
                                        null
                                    },
                                    onImageClicked = { imageMessage, _, _ -> images.add(imageMessage.header.messageId) },
                                    onLinkClicked = { links.add(it) },
                                    onProfileClicked = { profiles.add(it) },
                                ),
                                onKeyboardOptionsRequested = if (withOwner && optionsAvailable) {
                                    {
                                        opened.add(it.header.messageId)
                                        if (!noShow) lastOpeningJob = optionsState.open(it.header.messageId, fromKeyboard = true)
                                    }
                                } else {
                                    null
                                },
                                retainMessageNavigationForOptions = withOwner && optionsState.retainMessageNavigation,
                                messageOptionsDismissal = optionsState.dismissal.takeIf { withOwner },
                                onMessageOptionsDismissalHandled = optionsState::acknowledgeDismissal,
                                modifier = Modifier.weight(1f).testTag("list"),
                                isBubbleUiEnabled = bubbles,
                            )
                            var value by remember { mutableStateOf("") }
                            BasicTextField(value, { value = it }, Modifier.testTag("composer").focusRequester(composerFocus))
                        }
                        MessageOptionsModalSheetLayout(
                            conversationId = regular.conversationId,
                            optionsState = optionsState,
                            isNetworkAvailable = true,
                            onCopyClick = {},
                            onDeleteClick = { _, _ -> },
                            onReactionClick = { _, emoji -> reactions.add(emoji) },
                            onDetailsClick = { _, _ -> composerFocus.requestFocus() },
                            onReplyClick = { composerFocus.requestFocus() },
                            onEditClick = { _, _, _, _ -> composerFocus.requestFocus() },
                            onShareAssetExternallyClick = {},
                            onShareAssetViaWireClick = {},
                            onDownloadAssetClick = {},
                            onOpenAssetClick = {},
                            viewModel = viewModel,
                        )
                    }
                    val actualWindowInfo = LocalWindowInfo.current
                    val windowInfo = remember(actualWindowInfo, windowFocused) {
                        object : WindowInfo by actualWindowInfo {
                            override val isWindowFocused: Boolean
                                get() = windowFocused?.value ?: actualWindowInfo.isWindowFocused
                        }
                    }
                    CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
                        content()
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Composable
    private fun rememberTestOptionsState(scheduler: TestCoroutineScheduler?, onTouchDismiss: () -> Unit): MessageOptionsState {
        if (scheduler == null) return rememberMessageOptionsState(onTouchDismiss = onTouchDismiss, keyboard = null)
        val scope = rememberCoroutineScope { StandardTestDispatcher(scheduler) }
        val keyboard = remember {
            object : SoftwareKeyboardController {
                override fun show() = Unit
                override fun hide() = Unit
            }
        }
        return rememberMessageOptionsState(onTouchDismiss = onTouchDismiss, scope = scope, keyboard = keyboard)
    }

    private fun focusMessage() {
        rule.onNodeWithContentDescription("Conversation messages.", substring = true)
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        key(Key.Enter)
        rule.onNodeWithTag("message_target").assertIsFocused()
    }

    private fun shortcut(shifted: Boolean) = key(if (shifted) Key.F10 else Key.Menu, shifted)

    private fun focusInline(text: String) {
        rule.onNode(hasClickAction() and hasAnyAncestor(hasText(text)), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
    }

    private fun focusHandle() {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            rule.waitUntil(5_000) {
                WindowInspector.getGlobalWindowViews().filter {
                    (it.layoutParams as? android.view.WindowManager.LayoutParams)?.type ==
                        android.view.WindowManager.LayoutParams.TYPE_APPLICATION
                }.lastOrNull()?.hasWindowFocus() == true
            }
        }
        rule.onNodeWithContentDescription("Drag handle").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
    }

    private fun activateMenuItem(text: String) {
        rule.onNode(hasText(text) and hasAnyAncestor(isDialog())).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        key(Key.Enter)
    }

    private fun dismissSheet() {
        focusHandle()
        key(Key.Enter)
    }

    private fun key(key: Key, shifted: Boolean = false) {
        // Target the actual Compose owner, rather than a tooltip root or an Android window
        // that may still be closing. Native picker input and live UHID checks cover the bridge.
        val owner = if (sheet.isVisible) {
            rule.onNodeWithContentDescription("Drag handle")
        } else {
            rule.onNodeWithContentDescription("Conversation messages.", substring = true)
        }
        owner.performKeyInput {
            if (shifted) keyDown(Key.ShiftLeft)
            keyDown(key)
            keyUp(key)
            if (shifted) keyUp(Key.ShiftLeft)
        }
        rule.waitForIdle()
        if (hostFocusOverride?.value == false) return
        if (!sheet.isVisible && !optionsState.isOpening && optionsState.retainMessageNavigation) {
            // Android may restore its previous focus before our stable-ID restoration finishes.
            rule.waitUntil(5_000) { !optionsState.retainMessageNavigation }
            rule.waitForIdle()
        }
    }

    private fun nativeEvent(keyCode: Int, modifiers: Int = 0, repeat: Int = 0) =
        KeyEvent(AndroidKeyEvent(0, 0, AndroidKeyEvent.ACTION_DOWN, keyCode, repeat, modifiers))
}
