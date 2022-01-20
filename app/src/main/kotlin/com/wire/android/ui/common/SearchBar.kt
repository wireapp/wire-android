package com.wire.android.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.theme.WireColor

@Composable
fun SearchBarUI(placeholderText: String, modifier: Modifier = Modifier, onTextTyped: (String) -> Unit = {}) {
    var showClearButton by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    OutlinedTextField(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.onSecondary, RoundedCornerShape(40.dp))
            .border(1.dp, Color(0xFFDCE0E3), RoundedCornerShape(40.dp)),
        value = text,
        onValueChange = {
            text = it
            onTextTyped(it)
            showClearButton = it.isNotEmpty()
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                tint = WireColor.Dark80Gray,
                contentDescription = stringResource(R.string.content_description_clear_content)
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = showClearButton,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(onClick = {
                    text = ""
                    showClearButton = false
                }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.content_description_clear_content)
                    )
                }
            }
        },

        placeholder = {
            Text(
                text = placeholderText,
                modifier = Modifier.fillMaxWidth(),
                style = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
            )
        },
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = Transparent,
            unfocusedIndicatorColor = Transparent,
            backgroundColor = MaterialTheme.colors.onSecondary,
            cursorColor = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
        ),
        maxLines = 1,
        singleLine = true,
    )
}

@Composable
fun NoSearchResults() {
    Column(
        modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        Text(stringResource(R.string.search_no_results))
    }
}

@Preview(showBackground = true)
@Composable
fun SearchBarCollapsedPreview() {
    SearchBarUI("Search text")
}
