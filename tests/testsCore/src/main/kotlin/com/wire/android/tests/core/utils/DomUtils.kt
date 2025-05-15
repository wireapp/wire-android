/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.tests.core.utils

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory


class DomUtils {

    fun toDocument(message: String?): Document {
        val docBuilderFactory = DocumentBuilderFactory.newInstance()
        docBuilderFactory.isNamespaceAware = true
        try {
            val docBuilder = docBuilderFactory.newDocumentBuilder()
            return docBuilder.parse(InputSource(StringReader(message)))
        } catch (e: SAXException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: ParserConfigurationException) {
            throw RuntimeException(e)
        }
    }

    fun extractXpath(document: Document?, xpathString: String?): Node {
        val factory = XPathFactory.newInstance()
        val xpath = factory.newXPath()
        try {
            return xpath.evaluate(xpathString, document, XPathConstants.NODE) as Node
        } catch (e: XPathExpressionException) {
            throw RuntimeException(e)
        }
    }

    fun toString(element: Node?): String {
        try {
            val source = DOMSource(element)
            val stringResult = StringWriter()
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty("omit-xml-declaration", "yes")
            transformer.setOutputProperty("indent", "yes")
            transformer.transform(source, StreamResult(stringResult))
            return stringResult.toString()
        } catch (e: TransformerException) {
            throw RuntimeException(e)
        }
    }
}
