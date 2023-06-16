/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.node.Node

@Composable
fun MDTable(tableBlock: TableBlock, nodeData: NodeData) {
    val tableData = mutableListOf<List<AnnotatedString>>()
    var child = tableBlock.firstChild
    // Parse the table block
    while (child != null) {
        when (child) {
            is TableHead -> {
                var rowNode = child.firstChild
                while (rowNode != null) {
                    val row = parseRow(rowNode, nodeData)
                    tableData.add(row)
                    rowNode = rowNode.next
                }
            }

            is TableBody -> {
                var rowNode = child.firstChild
                while (rowNode != null) {
                    val row = parseRow(rowNode, nodeData)
                    tableData.add(row)
                    rowNode = rowNode.next
                }
            }
        }
        child = child.next
    }

    val columnCount = tableData.firstOrNull()?.size ?: 0

    // Create a table
    Column {
        tableData.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (rowIndex == 0) Color.LightGray else Color.White)
            ) {
                for (columnIndex in 0 until columnCount) {
                    Text(
                        text = row[columnIndex].toString(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                            .border(0.5.dp, Color.Gray)
                    )
                }
            }
        }
    }
}

private fun parseRow(tableRow: Node, nodeData: NodeData): List<AnnotatedString> {
    val row = mutableListOf<AnnotatedString>()
    var child = tableRow.firstChild
    while (child != null) {
        if (child is TableCell) {
            val cellText = buildAnnotatedString {
                inlineChildren(child, this, nodeData)
            }
            row.add(cellText)
        }
        child = child.next
    }
    return row
}
