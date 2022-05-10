package com.wire.android.ui.common.bottomsheet

import androidx.activity.compose.BackHandler
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
    fun hide() {
        coroutineScope.launch { sheetState.animateTo(ModalBottomSheetValue.Hidden) }
    }
    BackHandler(enabled = sheetState.isVisible) {
        hide()
    }
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
    coroutineScope: CoroutineScope,
    headerTitle: String? = null,
    headerIcon: @Composable () -> Unit = {},
    menuItems: List<@Composable () -> Unit>,
    content: @Composable () -> Unit,
) {
    WireModalSheetLayout(
        sheetState = sheetState,
        coroutineScope = coroutineScope,
        sheetContent = {
            MenuModalSheetContent(
                headerTitle,
                headerIcon,
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
    menuItems: List<@Composable () -> Unit>,
    headerModifier: Modifier = Modifier
) {
    ModalSheetHeaderItem(
        title = headerTitle,
        leadingIcon = headerIcon,
        modifier = headerModifier
    )

    buildMenuSheetItems(items = menuItems)
}
