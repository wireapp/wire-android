package com.wire.android.ui.common.bottomsheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WireModalSheetLayout(
    sheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
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
        },
        sheetBackgroundColor = colorsScheme().surface
    ) {
        content()
    }

    BackHandler(enabled = sheetState.isVisible) {
        coroutineScope.launch { sheetState.animateTo(ModalBottomSheetValue.Hidden) }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MenuModalSheetLayout(
    sheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    header: MenuModalSheetHeader = MenuModalSheetHeader.Gone,
    menuItems: List<@Composable () -> Unit>,
    content: @Composable () -> Unit,
) {
    WireModalSheetLayout(
        sheetState = sheetState,
        coroutineScope = coroutineScope,
        sheetContent = { MenuModalSheetContent(header, menuItems) },
        content = { content() }
    )
}


@Composable
fun MenuModalSheetContent(
    header: MenuModalSheetHeader = MenuModalSheetHeader.Gone,
    menuItems: List<@Composable () -> Unit>
) {
    ModalSheetHeaderItem(header = header)
    buildMenuSheetItems(items = menuItems)
}
