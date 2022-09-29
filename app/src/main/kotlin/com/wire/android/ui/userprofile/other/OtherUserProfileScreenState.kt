package com.wire.android.ui.userprofile.other

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberOtherUserProfileScreenState(
    otherUserBottomSheetContentState: OtherUserBottomSheetContentState
): OtherUserProfileScreenState {
    val context = LocalContext.current
    val clipBoardManager = LocalClipboardManager.current

    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    val snackBarHostState: SnackbarHostState = remember { SnackbarHostState() }

    val blockUserDialogState = rememberVisibilityState<BlockUserDialogState>()
    val unblockUserDialogState = rememberVisibilityState<UnblockUserDialogState>()
    val removeMemberDialogState = rememberVisibilityState<RemoveConversationMemberState>()

    return remember {
        OtherUserProfileScreenState(
            context = context,
            clipBoardManager = clipBoardManager,
            coroutineScope = coroutineScope,
            otherUserBottomSheetContentState = otherUserBottomSheetContentState,
            snackbarHostState = snackBarHostState,
            blockUserDialogState = blockUserDialogState,
            unblockUserDialogState = unblockUserDialogState,
            removeMemberDialogState = removeMemberDialogState
        )
    }
}

data class OtherUserProfileScreenState(
    private val context: Context,
    private val clipBoardManager: ClipboardManager,
    val coroutineScope: CoroutineScope,
    val otherUserBottomSheetContentState: OtherUserBottomSheetContentState,
    val snackbarHostState: SnackbarHostState,
    val blockUserDialogState: VisibilityState<BlockUserDialogState>,
    val unblockUserDialogState: VisibilityState<UnblockUserDialogState>,
    val removeMemberDialogState: VisibilityState<RemoveConversationMemberState>
) {
    fun copy(text: String) {
        clipBoardManager.setText(AnnotatedString(text))
        coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.label_value_copied, text)) }
    }

    fun showConversationOption() {
        coroutineScope.launch {
            otherUserBottomSheetContentState.showConversationOption()
        }
    }

    fun showChangeRoleOption() {
        coroutineScope.launch {
            otherUserBottomSheetContentState.showChangeRoleOption()
        }
    }

    fun closeBottomSheet() {
        coroutineScope.launch { otherUserBottomSheetContentState.hide() }
    }

    fun showSnackbar(uiText: UIText) {
        coroutineScope.launch { snackbarHostState.showSnackbar(uiText.asString(context.resources)) }
    }

    fun dismissDialogs() {
        blockUserDialogState.dismiss()
        unblockUserDialogState.dismiss()
        removeMemberDialogState.dismiss()
    }
}

@OptIn(ExperimentalPagerApi::class)
data class OtherUserProfilePagerState(
    val pagerState: PagerState,
    val tabBarElevationState: Dp,
    val tabItems: List<OtherUserProfileTabItem>,
    val currentTabState: Int,
    val tabItemsLazyListState: Map<OtherUserProfileTabItem, LazyListState>,
    val topBarMaxBarElevation: Dp
)

@OptIn(ExperimentalPagerApi::class)
@Composable
fun rememberOtherUserProfilePagerState(showGroupOption: Boolean): OtherUserProfilePagerState {
    val pagerState = rememberPagerState()

    val tabItems = remember(showGroupOption) {
        buildList {
            if (showGroupOption) add(OtherUserProfileTabItem.GROUP)
            add(OtherUserProfileTabItem.DETAILS)
            add(OtherUserProfileTabItem.DEVICES)
        }
    }

    val tabItemsLazyListState = tabItems.associateWith { rememberLazyListState() }

    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }

    val topBarMaxBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    val tabBarElevationState by remember(tabItems, tabItemsLazyListState, currentTabState) {
        derivedStateOf {
            tabItemsLazyListState[tabItems[currentTabState]]?.topBarElevation(topBarMaxBarElevation) ?: 0.dp
        }
    }

    return remember(currentTabState, tabItems, tabBarElevationState) {
        OtherUserProfilePagerState(
            pagerState = pagerState,
            tabBarElevationState = tabBarElevationState,
            tabItems = tabItems,
            currentTabState = currentTabState,
            tabItemsLazyListState = tabItemsLazyListState,
            topBarMaxBarElevation = topBarMaxBarElevation
        )
    }

}

