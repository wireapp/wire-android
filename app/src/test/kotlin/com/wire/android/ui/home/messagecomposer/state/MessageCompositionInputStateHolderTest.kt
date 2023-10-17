package com.wire.android.ui.home.messagecomposer.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import com.wire.android.config.CoroutineTestExtension
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MessageCompositionInputStateHolderTest {

    private lateinit var messageComposition: MutableState<MessageComposition>

    private lateinit var state: MessageCompositionInputStateHolder

    @BeforeEach
    fun before() {
        messageComposition = mutableStateOf(MessageComposition())
        state = MessageCompositionInputStateHolder(
            messageComposition = messageComposition,
            selfDeletionTimer = mutableStateOf(SelfDeletionTimer.Disabled)
        )
    }

    @Test
    fun `when IME is visible, showOptions should be set to true`() {
        // Given
        val isImeVisible = true

        // When
        state.handleIMEVisibility(isImeVisible)

        // Then
        state.optionsVisible shouldBeEqualTo true
    }

    @Test
    fun `when IME is hidden and showSubOptions is true, showOptions remains unchanged`() {
        // Given
        val isImeVisible = false
        state.updateValuesForTesting(showSubOptions = true, showOptions = false)

        // When
        state.handleIMEVisibility(isImeVisible)

        // Then
        state.optionsVisible shouldBeEqualTo false
    }

    @Test
    fun `when IME is hidden and showSubOptions is false, showOptions should be set to false`() {
        // Given
        val isImeVisible = false
        state.updateValuesForTesting(showSubOptions = false)

        // When
        state.handleIMEVisibility(isImeVisible)

        // Then
        state.optionsVisible shouldBeEqualTo false
    }

    @Test
    fun `when offset increases and is bigger than previous and options height, options height is updated`() {
        // When
        state.handleOffsetChange(
            50.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.optionsHeight shouldBeEqualTo 50.dp
        state.subOptionsVisible shouldBeEqualTo false
    }

    @Test
    fun `when offset decreases and showSubOptions is false, options height is updated`() {
        // Given
        state.updateValuesForTesting(previousOffset = 50.dp)

        // When
        state.handleOffsetChange(
            20.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.optionsHeight shouldBeEqualTo 20.dp
    }

    @Test
    fun `when offset decreases to zero, showOptions and isTextExpanded are set to false`() {
        // Given
        state.updateValuesForTesting(previousOffset = 50.dp)

        // When
        state.handleOffsetChange(
            0.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.optionsVisible shouldBeEqualTo false
        state.isTextExpanded shouldBeEqualTo false
    }

    @Test
    fun `when offset equals keyboard height, showSubOptions is set to false`() {
        // Given
        state.updateValuesForTesting(keyboardHeight = 30.dp)

        // When
        state.handleOffsetChange(
            30.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.subOptionsVisible shouldBeEqualTo false
    }

    @Test
    fun `when offset is greater than keyboard height, keyboardHeight is updated`() {
        // Given
        state.updateValuesForTesting(keyboardHeight = 20.dp)

        // When
        state.handleOffsetChange(
            30.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.keyboardHeight shouldBeEqualTo 30.dp
    }

    @Test
    fun `when offset increases and is greater than keyboardHeight but is less than previousOffset, keyboardHeight is updated`() {
        // Given
        state.updateValuesForTesting(previousOffset = 50.dp, keyboardHeight = 20.dp)

        // When
        state.handleOffsetChange(
            30.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.keyboardHeight shouldBeEqualTo 30.dp
        state.optionsHeight shouldBeEqualTo 30.dp
    }

    @Test
    fun `when offset decreases, showSubOptions is true, and actualOffset is greater than optionsHeight, values remain unchanged`() {
        // Given
        state.updateValuesForTesting(
            previousOffset = 50.dp,
            keyboardHeight = 20.dp,
            showSubOptions = true,
            optionsHeight = 10.dp
        )

        // When
        state.handleOffsetChange(
            30.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.optionsHeight shouldBeEqualTo 10.dp
    }

    @Test
    fun `when offset decreases, showSubOptions is false, and actualOffset is greater than optionsHeight, optionsHeight is updated`() {
        // Given
        state.updateValuesForTesting(
            previousOffset = 50.dp,
            keyboardHeight = 20.dp,
            showSubOptions = false,
            optionsHeight = 10.dp
        )

        // When
        state.handleOffsetChange(
            30.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.optionsHeight shouldBeEqualTo 30.dp
    }

    @Test
    fun `when offset is the same as previousOffset and greater than current keyboardHeight, keyboardHeight is updated`() {
        // Given
        state.updateValuesForTesting(previousOffset = 40.dp, keyboardHeight = 20.dp)

        // When
        state.handleOffsetChange(
            40.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.keyboardHeight shouldBeEqualTo 40.dp
        state.optionsHeight shouldBeEqualTo 0.dp
    }

    @Test
    fun `given first keyboard appear when source equals target, then initialKeyboardHeight is set`() {
        // Given
        val imeValue = 50.dp
        state.updateValuesForTesting(initialKeyboardHeight = 0.dp)

        // When
        state.handleOffsetChange(20.dp, NAVIGATION_BAR_HEIGHT, source = imeValue, target = imeValue)

        // Then
        state.initialKeyboardHeight shouldBeEqualTo imeValue
    }

    @Test
    fun `given extended keyboard height when attachment button is clicked, then keyboardHeight is set to initialKeyboardHeight`() {
        // Given
        val initialKeyboardHeight = 10.dp
        state.updateValuesForTesting(previousOffset = 40.dp, keyboardHeight = 20.dp, initialKeyboardHeight = initialKeyboardHeight)

        // When
        state.showOptions()
        state.handleOffsetChange(0.dp, NAVIGATION_BAR_HEIGHT, source = TARGET, target = SOURCE)

        // Then
        state.keyboardHeight shouldBeEqualTo 20.dp
        state.optionsHeight shouldBeEqualTo initialKeyboardHeight
    }

    @Test
    fun `when offset decreases but is not zero, only optionsHeight is updated`() {
        // Given
        state.updateValuesForTesting(previousOffset = 50.dp)

        // When
        state.handleOffsetChange(
            10.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.optionsHeight shouldBeEqualTo 10.dp
        state.optionsVisible shouldBeEqualTo false
        state.isTextExpanded shouldBeEqualTo false
    }

    companion object {
        // I set it 0 to make tests more straight forward
        val NAVIGATION_BAR_HEIGHT = 0.dp
        val SOURCE = 0.dp
        val TARGET = 50.dp
    }
}
