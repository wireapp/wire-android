package com.wire.android.ui.home.messagecomposer

import android.content.Context
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MessageComposerInnerStateTest {

    @MockK
    lateinit var context: Context

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `when @ symbol is added into message, then mention is queried`() = runTest {
        val state = createState(context)
        state.setMessageTextValue(textFieldValueWithSelection("start text"))

        state.setMessageTextValue(textFieldValueWithSelection("start text @"))

        assertEquals("", state.mentionQueryFlowState.value)
    }

    @Test
    fun `when @ symbol is added into message without space before it, then mention is not queried`() = runTest {
        val state = createState(context)
        state.setMessageTextValue(textFieldValueWithSelection("start text"))

        state.setMessageTextValue(textFieldValueWithSelection("start text@"))

        assertEquals(null, state.mentionQueryFlowState.value)
    }

    @Test
    fun `when mention is started in message, then mention is queried with corresponding query`() = runTest {
        val state = createState(context)
        state.setMessageTextValue(textFieldValueWithSelection("start text @"))

        state.setMessageTextValue(textFieldValueWithSelection("start text @abc"))

        assertEquals("abc", state.mentionQueryFlowState.value)
    }

    @Test
    fun `when mention is started in message and user type space, then mention stop querying`() = runTest {
        val state = createState(context)
        state.setMessageTextValue(textFieldValueWithSelection("start text @"))

        state.setMessageTextValue(textFieldValueWithSelection("start text @abc"))
        assertEquals("abc", state.mentionQueryFlowState.value)

        state.setMessageTextValue(textFieldValueWithSelection("start text @abc "))
        assertEquals(null, state.mentionQueryFlowState.value)
    }

    @Test
    fun `when mention selected, then mention is added into list`() = runTest {
        val state = createState(context)
        state.setMessageTextValue(TextFieldValue("start text @use testing", TextRange(15)))

        state.addMention(contact())

        val expectedMention = listOf(UiMention(11, 11, UserId("id_0", "domain"), "@user name0"))
        assertEquals(expectedMention, state.mentions)
        assertEquals("start text @user name0 testing", state.messageText.text)
    }

    @Test
    fun `when text before mention was changed, then mention's position is updated`() = runTest {
        val state = createState(context)
        state.setMessageTextValue(TextFieldValue("start text @use testing", TextRange(15)))
        state.addMention(contact())

        state.setMessageTextValue(TextFieldValue("start text changed @user name0 testing", TextRange(18)))

        val expected = listOf(UiMention(19, 11, UserId("id_0", "domain"), "@user name0"))
        assertEquals(expected, state.mentions)
    }

    @Test
    fun `when text before mention was changed 2, then mention's position is updated`() = runTest {
        val state = createState(context)
        state.setMessageTextValue(TextFieldValue("start text @use testing", TextRange(15)))
        state.addMention(contact())

        state.setMessageTextValue(TextFieldValue("@user name0 testing", TextRange(0)))

        val expected = listOf(UiMention(0, 11, UserId("id_0", "domain"), "@user name0"))
        assertEquals(expected, state.mentions)
    }

    @Test
    fun `when mention-text was edited, then mention is removed from the list`() = runTest {
        val state = createState(context)
        state.setMessageTextValue(textFieldValueWithSelection("start text @"))
        state.addMention(contact())

        state.setMessageTextValue(textFieldValueWithSelection("start text @user na"))

        val expected = listOf<UiMention>()
        assertEquals(expected, state.mentions)
    }

    // case was found by manual testing
    @Test
    fun `when message starts from mention and then it's removed, then mention is not requested anymore`() = runTest {
        val state = createState(context)
        state.setMessageTextValue(textFieldValueWithSelection("@"))

        assertEquals("", state.mentionQueryFlowState.value)
        state.setMessageTextValue(textFieldValueWithSelection(""))

        assertEquals(null, state.mentionQueryFlowState.value)
    }

    // case was found by manual testing
    @Test
    fun `when selection goes just before mention symbol, then mention is not requested`() = runTest {
        val state = createState(context)
        state.setMessageTextValue(textFieldValueWithSelection("@ @ "))

        state.setMessageTextValue(TextFieldValue("@ @ ", TextRange(3)))

        assertEquals("", state.mentionQueryFlowState.value)

        state.setMessageTextValue(TextFieldValue("@ @ ", TextRange(2)))

        assertEquals(null, state.mentionQueryFlowState.value)
    }

    // case was found by manual testing
    @Test
    fun `when mention symbol is at the begin of new line, then mention is requested`() = runTest {
        val state = createState(context)
        state.setMessageTextValue(textFieldValueWithSelection("some text\n"))

        state.setMessageTextValue(textFieldValueWithSelection("some text\n@"))

        assertEquals("", state.mentionQueryFlowState.value)
    }

    // case was found by manual testing
    @Test
    fun `when cursor is at the begin of new line, and add mention btn clicked, then mention is requested`() = runTest {
        val state = createState(context)
        state.setMessageTextValue(textFieldValueWithSelection("some text\n"))
        state.startMention()

        assertEquals("", state.mentionQueryFlowState.value)
        assertEquals("some text\n@", state.messageText.text)
    }

    private fun textFieldValueWithSelection(text: String) = TextFieldValue(text, TextRange(text.length))

    private fun contact(suffix: Int = 0) = Contact(
        id = "id_$suffix",
        domain = "domain",
        name = "user name$suffix",
        connectionState = ConnectionState.ACCEPTED,
        membership = Membership.Guest
    )

    companion object {
        fun createState(context: Context) = MessageComposerInnerState(0.dp, AttachmentInnerState(context), {}, SpanStyle())
    }
}
