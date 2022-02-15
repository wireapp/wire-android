package com.wire.android.ui.common.bottomsheet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.wireDimensions

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WireModalSheetLayout(
    sheetState: ModalBottomSheetState,
    sheetShape: Shape = androidx.compose.material.MaterialTheme.shapes.large.copy(
        topStart = CornerSize(MaterialTheme.wireDimensions.conversationBottomSheetShapeCorner),
        topEnd = CornerSize(MaterialTheme.wireDimensions.conversationBottomSheetShapeCorner)
    ),
    sheetContent: @Composable ColumnScope.() -> Unit,
    content: @Composable () -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = sheetState,
        //TODO: create a shape object inside the materialtheme 3 component
        sheetShape = sheetShape,
        sheetContent = {
            Spacer(modifier = Modifier.height(8.dp))
            Divider(
                modifier = Modifier
                    .width(width = 48.dp)
                    .align(alignment = Alignment.CenterHorizontally),
                thickness = 4.dp
            )

            sheetContent()
        }
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MenuModalSheetLayout(
    sheetState: ModalBottomSheetState,
    headerTitle: String,
    headerIcon: (@Composable () -> Unit)? = null,
    menuItems: List<@Composable () -> Unit>,
    content: @Composable () -> Unit,
) {
    WireModalSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            ModalSheetHeaderItem(
                title = headerTitle,
                leadingIcon = headerIcon
            )

            buildMenuSheetItems(items = menuItems)
        }
    ) {
        content()
    }
}
