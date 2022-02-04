//package com.wire.android.ui.home.messagecomposer
//
//import androidx.compose.animation.core.TweenSpec
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.gestures.Orientation
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.BoxWithConstraints
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.ColumnScope
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.offset
//import androidx.compose.material.ExperimentalMaterialApi
//import androidx.compose.material.MaterialTheme
//import androidx.compose.material.ModalBottomSheetDefaults
//import androidx.compose.material.ModalBottomSheetState
//import androidx.compose.material.ModalBottomSheetValue
//import androidx.compose.material.Scrim
//import androidx.compose.material.Strings
//import androidx.compose.material.Surface
//import androidx.compose.material.bottomSheetSwipeable
//import androidx.compose.material.contentColorFor
//import androidx.compose.material.getString
//import androidx.compose.material.rememberModalBottomSheetState
//import androidx.compose.material.swipeable
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.State
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Shape
//import androidx.compose.ui.graphics.isSpecified
//import androidx.compose.ui.input.nestedscroll.nestedScroll
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.layout.onGloballyPositioned
//import androidx.compose.ui.semantics.collapse
//import androidx.compose.ui.semantics.contentDescription
//import androidx.compose.ui.semantics.dismiss
//import androidx.compose.ui.semantics.expand
//import androidx.compose.ui.semantics.onClick
//import androidx.compose.ui.semantics.semantics
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.IntOffset
//import kotlinx.coroutines.launch
//import kotlin.math.max
//import kotlin.math.roundToInt
//
//@Composable
//@ExperimentalMaterialApi
//fun ModalBottomSheetLayout(
//    sheetContent: @Composable ColumnScope.() -> Unit,
//    modifier: Modifier = Modifier,
//    sheetState: ModalBottomSheetState =
//        rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
//    sheetShape: Shape = MaterialTheme.shapes.large,
//    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
//    sheetBackgroundColor: Color = MaterialTheme.colors.surface,
//    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
//    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
//    content: @Composable () -> Unit
//) {
//    val scope = rememberCoroutineScope()
//    BoxWithConstraints(modifier) {
//        val fullHeight = constraints.maxHeight.toFloat()
//        val sheetHeightState = remember { mutableStateOf<Float?>(null) }
//        Box(Modifier.fillMaxSize()) {
//            content()
//            Scrim(
//                color = scrimColor,
//                onDismiss = {
//                    if (sheetState.confirmStateChange(ModalBottomSheetValue.Hidden)) {
//                        scope.launch { sheetState.hide() }
//                    }
//                },
//                visible = sheetState.targetValue != ModalBottomSheetValue.Hidden
//            )
//        }
//        Surface(
//            Modifier
//                .fillMaxWidth()
//                .nestedScroll(sheetState.nestedScrollConnection)
//                .offset {
//                    val y = if (sheetState.anchors.isEmpty()) {
//                        // if we don't know our anchors yet, render the sheet as hidden
//                        fullHeight.roundToInt()
//                    } else {
//                        // if we do know our anchors, respect them
//                        sheetState.offset.value.roundToInt()
//                    }
//                    IntOffset(0, y)
//                }
//                .bottomSheetSwipeable(sheetState, fullHeight, sheetHeightState)
//                .onGloballyPositioned {
//                    sheetHeightState.value = it.size.height.toFloat()
//                }
//                .semantics {
//                    if (sheetState.isVisible) {
//                        dismiss {
//                            if (sheetState.confirmStateChange(ModalBottomSheetValue.Hidden)) {
//                                scope.launch { sheetState.hide() }
//                            }
//                            true
//                        }
//                        if (sheetState.currentValue == ModalBottomSheetValue.HalfExpanded) {
//                            expand {
//                                if (sheetState.confirmStateChange(ModalBottomSheetValue.Expanded)) {
//                                    scope.launch { sheetState.expand() }
//                                }
//                                true
//                            }
//                        } else if (sheetState.isHalfExpandedEnabled) {
//                            collapse {
//                                if (sheetState.confirmStateChange(ModalBottomSheetValue.HalfExpanded)) {
//                                    scope.launch { sheetState.halfExpand() }
//                                }
//                                true
//                            }
//                        }
//                    }
//                },
//            shape = sheetShape,
//            elevation = sheetElevation,
//            color = sheetBackgroundColor,
//            contentColor = sheetContentColor
//        ) {
//            Column(content = sheetContent)
//        }
//    }
//}
//
//@Suppress("ModifierInspectorInfo")
//@OptIn(ExperimentalMaterialApi::class)
//private fun Modifier.bottomSheetSwipeable(
//    sheetState: ModalBottomSheetState,
//    fullHeight: Float,
//    sheetHeightState: State<Float?>
//): Modifier {
//    val sheetHeight = sheetHeightState.value
//    val modifier = if (sheetHeight != null) {
//        val anchors = if (sheetHeight < fullHeight / 2) {
//            mapOf(
//                fullHeight to ModalBottomSheetValue.Hidden,
//                fullHeight - sheetHeight to ModalBottomSheetValue.Expanded
//            )
//        } else {
//            mapOf(
//                fullHeight to ModalBottomSheetValue.Hidden,
//                fullHeight / 2 to ModalBottomSheetValue.HalfExpanded,
//                max(0f, fullHeight - sheetHeight) to ModalBottomSheetValue.Expanded
//            )
//        }
//        Modifier.swipeable(
//            state = sheetState,
//            anchors = anchors,
//            orientation = Orientation.Vertical,
//            enabled = sheetState.currentValue != ModalBottomSheetValue.Hidden,
//            resistance = null
//        )
//    } else {
//        Modifier
//    }
//
//    return this.then(modifier)
//}
//
//@Composable
//private fun Scrim(
//    color: Color,
//    onDismiss: () -> Unit,
//    visible: Boolean
//) {
//    if (color.isSpecified) {
//        val alpha by animateFloatAsState(
//            targetValue = if (visible) 1f else 0f,
//            animationSpec = TweenSpec()
//        )
//        val closeSheet = getString(Strings.CloseSheet)
//        val dismissModifier = if (visible) {
//            Modifier
//                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
//                .semantics(mergeDescendants = true) {
//                    contentDescription = closeSheet
//                    onClick { onDismiss(); true }
//                }
//        } else {
//            Modifier
//        }
//
//        Canvas(
//            Modifier
//                .fillMaxSize()
//                .then(dismissModifier)
//        ) {
//            drawRect(color = color, alpha = alpha)
//        }
//    }
//}
