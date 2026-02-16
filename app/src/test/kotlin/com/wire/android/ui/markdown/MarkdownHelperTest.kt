package com.wire.android.ui.markdown

import org.junit.jupiter.api.Assertions.assertEquals
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.user.UserId

class MarkdownHelperTest {

    @Test
    fun `given plain text node, when toContent is called, then it should return Inline Text`() {
        val textNode = Text("Sample text")

        val result = textNode.toContent()

        assert(result is MarkdownNode.Inline.Text)
        assertEquals("Sample text", (result as MarkdownNode.Inline.Text).literal)
    }

    @Test
    fun `given heading node, when toContent is called, then it should return Block Heading`() {
        val headLevel = 1
        val headingNode = Heading().apply { level = headLevel }
        headingNode.appendChild(Text("Header"))

        val result = headingNode.toContent()

        assert(result is MarkdownNode.Block.Heading)
        assertEquals(headLevel, (result as MarkdownNode.Block.Heading).level)
        assertEquals(headLevel, result.children.size)
    }

    @Test
    fun `given paragraph node, when toContent is called, then it should return Block Paragraph`() {
        val paragraphNode = Paragraph()
        paragraphNode.appendChild(Text("Sample paragraph"))

        val result = paragraphNode.toContent()

        assert(result is MarkdownNode.Block.Paragraph)
        assertEquals(1, result.children.size)
    }

    @Test
    fun `given ordered list node, when toContent is called, then it should return Block OrderedList`() {
        val orderedListNode = OrderedList()
        orderedListNode.appendChild(ListItem().apply { appendChild(Text("First item")) })
        orderedListNode.appendChild(ListItem().apply { appendChild(Text("Second item")) })

        val result = orderedListNode.toContent()

        assert(result is MarkdownNode.Block.ListBlock.Ordered)
        assertEquals(2, (result as MarkdownNode.Block.ListBlock.Ordered).children.size)
    }

    @Test
    fun `given document with text, when filterNodesContainingQuery is called with query, then it should return filtered document`() {
        val documentNode = Document()
        documentNode.appendChild(Paragraph().apply { appendChild(Text("Hello World")) })
        documentNode.appendChild(Paragraph().apply { appendChild(Text("Another paragraph")) })

        val result = documentNode.toContent().filterNodesContainingQuery("World")

        assert(result is MarkdownNode.Document)
        assertEquals(1, (result as MarkdownNode.Document).children.size)
        assert(result.children.first() is MarkdownNode.Block.Paragraph)
        assertEquals(
            "Hello World",
            ((result.children.first() as MarkdownNode.Block.Paragraph).children.first() as MarkdownNode.Inline.Text).literal
        )
    }

    @Test
    fun `given text with blank line, when toMarkdownDocument is called, then it should insert a spacer paragraph`() {
        val result = "test\n\ntest".toMarkdownDocument()

        assertEquals(3, result.children.size)
        val firstParagraph = result.children[0] as MarkdownNode.Block.Paragraph
        val spacerParagraph = result.children[1] as MarkdownNode.Block.Paragraph
        val lastParagraph = result.children[2] as MarkdownNode.Block.Paragraph
        assertEquals(1, firstParagraph.children.size)
        assertEquals(1, spacerParagraph.children.size)
        assertEquals(1, lastParagraph.children.size)
        assertTrue(firstParagraph.children[0] is MarkdownNode.Inline.Text)
        assertTrue(spacerParagraph.children[0] is MarkdownNode.Inline.Break)
        assertTrue(lastParagraph.children[0] is MarkdownNode.Inline.Text)
        assertEquals("test", (firstParagraph.children[0] as MarkdownNode.Inline.Text).literal)
        assertEquals("test", (lastParagraph.children[0] as MarkdownNode.Inline.Text).literal)
    }

    @Test
    fun `given text with two blank lines, when toMarkdownDocument is called, then it should preserve extra empty line`() {
        val result = "test\n\n\ntest".toMarkdownDocument()

        assertEquals(3, result.children.size)
        val firstParagraph = result.children[0] as MarkdownNode.Block.Paragraph
        val spacerParagraph = result.children[1] as MarkdownNode.Block.Paragraph
        val lastParagraph = result.children[2] as MarkdownNode.Block.Paragraph
        assertEquals(1, firstParagraph.children.size)
        assertEquals(2, spacerParagraph.children.size)
        assertEquals(1, lastParagraph.children.size)
        assertTrue(firstParagraph.children[0] is MarkdownNode.Inline.Text)
        assertTrue(spacerParagraph.children[0] is MarkdownNode.Inline.Break)
        assertTrue(spacerParagraph.children[1] is MarkdownNode.Inline.Break)
        assertTrue(lastParagraph.children[0] is MarkdownNode.Inline.Text)
        assertEquals("test", (firstParagraph.children[0] as MarkdownNode.Inline.Text).literal)
        assertEquals("test", (lastParagraph.children[0] as MarkdownNode.Inline.Text).literal)
    }

    @Test
    fun `given bullet list node, when toContent is called, then it should return Block BulletList`() {
        val bulletListNode = BulletList()
        bulletListNode.appendChild(ListItem().apply { appendChild(Text("First bullet")) })
        bulletListNode.appendChild(ListItem().apply { appendChild(Text("Second bullet")) })

        val result = bulletListNode.toContent()

        assert(result is MarkdownNode.Block.ListBlock.Bullet)
        assertEquals(2, (result as MarkdownNode.Block.ListBlock.Bullet).children.size)
    }

    @Test
    fun `given blockquote node, when toContent is called, then it should return Block BlockQuote`() {
        val blockQuoteNode = BlockQuote()
        blockQuoteNode.appendChild(Paragraph().apply { appendChild(Text("Quote text")) })

        val result = blockQuoteNode.toContent()

        assert(result is MarkdownNode.Block.BlockQuote)
        assertEquals(1, result.children.size)
    }

    @Test
    fun `given dynamic text with mention, when toMarkdownTextWithMentions is called, then it should wrap mention with markers`() {
        val text = "hi @john\n\nbye"
        val mention = MessageMention(
            start = 3,
            length = 5,
            userId = UserId("user-id", "domain"),
            isSelfMention = false
        )
        val uiText = UIText.DynamicString(text, listOf(mention))

        val (mentions, markedText) = uiText.toMarkdownTextWithMentions()

        assertEquals(1, mentions.size)
        assertEquals("@john", mentions.first().mentionUserName)
        assertTrue(
            markedText.contains(
                "${MarkdownConstants.MENTION_MARK}@john${MarkdownConstants.MENTION_MARK}"
            )
        )
    }

    @Test
    fun `given fenced code block, when toContent is called, then it should return Block FencedCode`() {
        val codeBlockNode = FencedCodeBlock()
        codeBlockNode.literal = "Sample code"

        val result = codeBlockNode.toContent()

        assert(result is MarkdownNode.Block.FencedCode)
        assertEquals("Sample code", (result as MarkdownNode.Block.FencedCode).literal)
    }

    @Test
    fun `given table block, when toContent is called, then it should return Block Table`() {
        val tableBlockNode = TableBlock()
        tableBlockNode.appendChild(
            TableHead().apply {
                appendChild(
                    TableRow()
                        .apply { appendChild(TableCell().apply { appendChild(Text("Header")) }) }
                )
            }
        )
        tableBlockNode.appendChild(
            TableBody().apply {
                appendChild(
                    TableRow()
                        .apply { appendChild(TableCell().apply { appendChild(Text("Cell")) }) }
                )
            }
        )

        val result = tableBlockNode.toContent()

        assert(result is MarkdownNode.Block.Table)
        assertEquals(2, (result as MarkdownNode.Block.Table).children.size)
    }

    @Test
    fun `given table row node, when toContent is called, then it should return TableRow`() {
        val rowNode = TableRow()
        rowNode.appendChild(TableCell())

        val result = rowNode.toContent()

        assert(result is MarkdownNode.TableRow)
        assertEquals(1, result.children.size)
    }

    @Test
    fun `given table cell node, when toContent is called, then it should return TableCell`() {
        val cellNode = TableCell()
        cellNode.appendChild(Text("cell content"))

        val result = cellNode.toContent()

        assert(result is MarkdownNode.TableCell)
        assertEquals(1, result.children.size)
    }

    @Test
    fun `given image node, when toContent is called, then it should return Inline Image`() {
        val imageNode = Image("image.png", "alt text")

        val result = imageNode.toContent()

        assert(result is MarkdownNode.Inline.Image)
        assertEquals("image.png", (result as MarkdownNode.Inline.Image).destination)
        assertEquals("alt text", result.title)
    }

    @Test
    fun `given emphasis node, when toContent is called, then it should return Inline Emphasis`() {
        val emphasisNode = Emphasis()
        emphasisNode.appendChild(Text("emphasized text"))

        val result = emphasisNode.toContent()

        assert(result is MarkdownNode.Inline.Emphasis)
        assertEquals(1, result.children.size)
    }

    @Test
    fun `given strong emphasis node, when toContent is called, then it should return Inline StrongEmphasis`() {
        val strongEmphasisNode = StrongEmphasis()
        strongEmphasisNode.appendChild(Text("strong text"))

        val result = strongEmphasisNode.toContent()

        assert(result is MarkdownNode.Inline.StrongEmphasis)
        assertEquals(1, result.children.size)
    }

    @Test
    fun `given link node, when toContent is called, then it should return Inline Link`() {
        val linkNode = Link("https://example.com", "Example")

        val result = linkNode.toContent()

        assert(result is MarkdownNode.Inline.Link)
        assertEquals("https://example.com", (result as MarkdownNode.Inline.Link).destination)
        assertEquals("Example", result.title)
    }

    @Test
    fun `given code node, when toContent is called, then it should return Inline Code`() {
        val codeNode = Code("inline code")

        val result = codeNode.toContent()

        assert(result is MarkdownNode.Inline.Code)
        assertEquals("inline code", (result as MarkdownNode.Inline.Code).literal)
    }

    @Test
    fun `given hard line break node, when toContent is called, then it should return Inline Break`() {
        val lineBreakNode = HardLineBreak()

        val result = lineBreakNode.toContent()

        assert(result is MarkdownNode.Inline.Break)
    }

    @Test
    fun `given indented code block, when toContent is called, then it should return Block IntendedCode`() {
        val codeBlockNode = IndentedCodeBlock()
        codeBlockNode.literal = "Sample indented code"

        val result = codeBlockNode.toContent()

        assert(result is MarkdownNode.Block.IntendedCode)
        assertEquals("Sample indented code", (result as MarkdownNode.Block.IntendedCode).literal)
    }

    @Test
    fun `given thematic break node, when toContent is called, then it should return Block ThematicBreak`() {
        val thematicBreakNode = ThematicBreak()

        val result = thematicBreakNode.toContent()

        assert(result is MarkdownNode.Block.ThematicBreak)
    }

    @Test
    fun `given strikethrough node, when toContent is called, then it should return Inline Strikethrough`() {
        val strikethroughNode = Strikethrough(null)
        strikethroughNode.appendChild(Text("strikethrough text"))

        val result = strikethroughNode.toContent()

        assert(result is MarkdownNode.Inline.Strikethrough)
        assertEquals(1, result.children.size)
    }

    @Test
    fun `given text without query, filterNodesContainingQuery should return null`() {
        val textNode = Text("This is a sample text without the query.").toContent()

        val result = textNode.filterNodesContainingQuery("longer query")

        assertNull(result)
    }

    @Test
    fun `given text with query, filterNodesContainingQuery should return non-null result`() {
        val textNode = Text("Sample text with query in the middle.").toContent()

        val result = textNode.filterNodesContainingQuery("query")

        assertNotNull(result)
    }

    @Test
    fun `given text with multiple queries, filterNodesContainingQuery should return non-null result`() {
        val textNode = Text("Query at the start and another query towards the end.").toContent()

        val result = textNode.filterNodesContainingQuery("query")

        assertNotNull(result)
    }

    @Test
    fun `given text with query case insensitive, filterNodesContainingQuery should return non-null result`() {
        val textNode = Text("Text with Query in mixed CASE.").toContent()

        val result = textNode.filterNodesContainingQuery("query")

        assertNotNull(result)
    }

    @Test
    fun `given text with query at the start, filterNodesContainingQuery should prepend ellipsis when necessary`() {
        val textNode = Text("query present at the very start of the text.").toContent()

        val result = textNode.filterNodesContainingQuery("query") as? MarkdownNode.Inline.Text

        assertNotNull(result)
        assertTrue(result!!.literal.startsWith("query"))
    }

    @Test
    fun `given text with query at the end, filterNodesContainingQuery should append ellipsis when necessary`() {
        val textNode = Text("Text ending with a query.").toContent()

        val result = textNode.filterNodesContainingQuery("query") as? MarkdownNode.Inline.Text

        assertNotNull(result)
        assertTrue(result!!.literal.endsWith("query."))
    }

    @Test
    fun `given text with multiple queries, filterNodesContainingQuery should include ellipsis between them`() {
        val textNode = Text("First query, some intermediate long text, second query.").toContent()

        val result = textNode.filterNodesContainingQuery("query") as? MarkdownNode.Inline.Text

        assertNotNull(result)
        assertTrue(result!!.literal.contains("..."))
    }

    @Test
    fun `given text with closely positioned queries, filterNodesContainingQuery should not include unnecessary ellipsis`() {
        val textNode = Text("First query and immediately second query.").toContent()

        val result = textNode.filterNodesContainingQuery("query") as? MarkdownNode.Inline.Text

        assertNotNull(result)
        assertFalse(result!!.literal.contains("..."))
    }
}
