package com.wire.android.ui.common.scaffold

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.common.snackbar.SnackBarViewModel
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireScaffold(
    viewModel: SnackBarViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: (@Composable (() -> Unit)?) = null,
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources

    // Snackbar logic for listening for any events from [ShowSnackBarUseCase]
    LaunchedEffect(Unit) {
        viewModel.snackBarMessage.collect { state ->
            state.message?.let {
                snackbarHostState.showSnackbar(it.asString(resources))
            }
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = {
            if(snackbarHost != null) {
                snackbarHost()
            } else {
                SwipeDismissSnackbarHost(snackbarHostState)
            }
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        contentColor = contentColor,
        content = content
    )
}
