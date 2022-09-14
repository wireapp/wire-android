package com.wire.android.ui.home.conversations.details

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
class GroupConversationDetailsState(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val keyboardController: SoftwareKeyboardController?,
    private val focusManager: FocusManager,
    val snackbarHostState: SnackbarHostState,
    val modalBottomSheetState: ModalBottomSheetState,
    val lazyListStates: List<LazyListState>,
    val pagerState: PagerState,
    val currentTabState: Int,
    val elevationState: Dp,
    val deleteGroupDialogState: VisibilityState<GroupDialogState>,
    val leaveGroupDialogState: VisibilityState<GroupDialogState>
) {

    fun showSnackbar(uiText: UIText) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(uiText.asString(context.resources))
        }
    }

    fun scrollToPage(page: Int) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(page)
        }
    }

    fun openBottomSheet() {
        coroutineScope.launch { modalBottomSheetState.show() }
    }

    fun closeBottomSheet() {
        coroutineScope.launch { modalBottomSheetState.hide() }
    }

    fun hideKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    fun openDeleteConversationDialog(groupDialogState: GroupDialogState) {
        deleteGroupDialogState.show(groupDialogState)
    }

    fun openLeaveConversationGroupDialog(groupDialogState: GroupDialogState) {
        leaveGroupDialogState.show(groupDialogState)
    }

}

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun rememberGroupConversationDetailsState(): GroupConversationDetailsState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val snackBarHostState = remember { SnackbarHostState() }

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    val lazyListStates: List<LazyListState> = GroupConversationDetailsTabItem.values().map { rememberLazyListState() }
    val pagerState = rememberPagerState(initialPage = GroupConversationDetailsTabItem.OPTIONS.ordinal)
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }

    val maxAppBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    val elevationState by remember { derivedStateOf { lazyListStates[currentTabState].topBarElevation(maxAppBarElevation) } }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val deleteGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val leaveGroupDialogState = rememberVisibilityState<GroupDialogState>()

    return remember(currentTabState, elevationState) {
        GroupConversationDetailsState(
            context,
            scope,
            keyboardController,
            focusManager,
            snackBarHostState,
            sheetState,
            lazyListStates,
            pagerState,
            currentTabState,
            elevationState,
            deleteGroupDialogState,
            leaveGroupDialogState
        )
    }
}
