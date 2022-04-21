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
import com.wire.android.ui.common.dimensions

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WireModalSheetLayout(
    sheetState: ModalBottomSheetState,
    sheetShape: Shape = androidx.compose.material.MaterialTheme.shapes.large.copy(
        topStart = CornerSize(dimensions().conversationBottomSheetShapeCorner),
        topEnd = CornerSize(dimensions().conversationBottomSheetShapeCorner)
    ),
    sheetContent: @Composable ColumnScope.() -> Unit,
    content: @Composable () -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = sheetShape,
        sheetContent = {
            Spacer(modifier = Modifier.height(8.dp))
            Divider(
                modifier = Modifier
                    .width(width = dimensions().modalBottomSheetDividerWidth)
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
    headerTitle: String? = null,
    headerIcon: @Composable () -> Unit = {},
    headerAction: () -> Unit = {},
    menuItems: List<@Composable () -> Unit>,
    content: @Composable () -> Unit,
) {
    WireModalSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            MenuModalSheetContent(
                headerTitle,
                headerIcon,
                headerAction,
                menuItems
            )
        }
    ) {
        content()
    }
}

@Composable
fun MenuModalSheetContent(
    headerTitle: String? = null,
    headerIcon: @Composable () -> Unit = {},
    headerAction: () -> Unit = {},
    menuItems: List<@Composable () -> Unit>,
) {
    ModalSheetHeaderItem(
        title = headerTitle,
        leadingIcon = headerIcon,
        iconAction = headerAction
    )

    buildMenuSheetItems(items = menuItems)
}
