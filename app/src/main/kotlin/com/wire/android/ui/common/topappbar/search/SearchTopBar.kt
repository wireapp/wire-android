package com.wire.android.ui.common.topappbar.search


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.R
import com.wire.android.ui.common.SearchBarInput
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SearchTopBar(
    isSearchActive: Boolean,
    searchBarHint: String,
    searchQuery: TextFieldValue = TextFieldValue(""),
    onSearchQueryChanged: (TextFieldValue) -> Unit,
    onInputClicked: () -> Unit,
    onCloseSearchClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .background(MaterialTheme.wireColorScheme.background)
    ) {
        val interactionSource = remember {
            MutableInteractionSource()
        }

        val focusManager = LocalFocusManager.current

        LaunchedEffect(isSearchActive) {
            if (!isSearchActive) {
                focusManager.clearFocus()
                onSearchQueryChanged(TextFieldValue(""))
            }
        }

        SearchBarInput(
            placeholderText = searchBarHint,
            text = searchQuery,
            onTextTyped = onSearchQueryChanged,
            leadingIcon = {
                AnimatedContent(!isSearchActive) { isVisible ->
                    IconButton(onClick = {
                        if (!isVisible) {
                            onCloseSearchClicked()
                        }
                    }) {
                        Icon(
                            painter = painterResource(
                                id = if (isVisible) R.drawable.ic_search
                                else R.drawable.ic_arrow_left
                            ),
                            contentDescription = stringResource(R.string.content_description_conversation_search_icon),
                            tint = MaterialTheme.wireColorScheme.onBackground
                        )
                    }
                }
            },
            placeholderTextStyle = textStyleAlignment(isTopBarVisible = !isSearchActive),
            textStyle = textStyleAlignment(isTopBarVisible = !isSearchActive),
            interactionSource = interactionSource,
            modifier = Modifier.padding(dimensions().spacing8x)
        )

        if (interactionSource.collectIsPressedAsState().value) {
            // we want to propagate the click on the input of the search
            // only the first time the user clicks on the input
            // that is when the search is not active
            if (!isSearchActive) {
                onInputClicked()
            }
        }
    }
}

@Composable
private fun textStyleAlignment(isTopBarVisible: Boolean): TextStyle {
    return if (isTopBarVisible) LocalTextStyle.current.copy(textAlign = TextAlign.Center) else LocalTextStyle.current.copy(
        textAlign = TextAlign.Start
    )
}

