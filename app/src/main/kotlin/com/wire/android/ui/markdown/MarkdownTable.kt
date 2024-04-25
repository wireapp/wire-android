/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun MarkdownTable(tableBlock: MarkdownNode.Block.Table, nodeData: NodeData, onMentionsUpdate: (List<DisplayMention>) -> Unit) {
    val tableData = mutableListOf<List<RowData>>()
    tableBlock.children.forEach { child ->
        when (child) {
            is MarkdownNode.Block.TableContent.Head -> {
                child.children.forEach { rowNode ->
                    val row = parseRowCells(rowNode.children, nodeData, true, onMentionsUpdate)
                    tableData.add(row)
                }
            }

            is MarkdownNode.Block.TableContent.Body -> {
                child.children.forEach { rowNode ->
                    val row = parseRowCells(rowNode.children, nodeData, false, onMentionsUpdate)
                    tableData.add(row)
                }
            }
        }
    }

    val columnCount by remember {
        mutableStateOf(tableData.firstOrNull()?.size ?: 0)
    }

    // Create a table
    Column(modifier = Modifier.padding(bottom = dimensions().spacing8x)) {
        tableData.map { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (row.firstOrNull()?.isHeader == true) {
                            MaterialTheme.wireColorScheme.outline
                        } else {
                            MaterialTheme.wireColorScheme.background
                        }
                    )
            ) {
                for (columnIndex in 0 until columnCount) {
                    MarkdownText(
                        annotatedString = row[columnIndex].annotatedString,
                        modifier = Modifier
                            .weight(1f)
                            .padding(dimensions().spacing8x),
                        onLongClick = nodeData.actions?.onLongClick,
                        onOpenProfile = nodeData.actions?.onOpenProfile
                    )
                }
            }
        }
    }
}

private fun parseRowCells(
    tableCells: List<MarkdownNode.TableCell>,
    nodeData: NodeData,
    isHeader: Boolean,
    onMentionsUpdate: (List<DisplayMention>) -> Unit
): List<RowData> {
    val rowsData = mutableListOf<RowData>()

    tableCells.forEach { child ->
        val cellText = buildAnnotatedString {
            onMentionsUpdate(inlineNodeChildren(child.children, this, nodeData))
        }
        rowsData.add(RowData(cellText, isHeader))
    }
    return rowsData
}

data class RowData(val annotatedString: AnnotatedString, val isHeader: Boolean)
