package com.wire.android.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.ui.common.textfield.WireTextField

@Composable
fun SearchBarUI(placeholderText: String, modifier: Modifier = Modifier, onTextTyped: (String) -> Unit = {}) {
    var showClearButton by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(TextFieldValue()) }

    WireTextField(
        modifier = modifier.padding(16.dp),
        value = text,

        onValueChange = {
            text = it
            onTextTyped(it.text)
            showClearButton = it.text.isNotEmpty()
        },
        leadingIcon = {
            IconButton(modifier = Modifier.height(40.dp), onClick = {}) {
                Icon(
                    Icons.Filled.Search,
                    stringResource(R.string.content_description_clear_content)
                )
            }
        },
        trailingIcon = {
            Box(modifier = Modifier.size(40.dp)) {
                AnimatedVisibility(
                    visible = showClearButton,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = {
                        text = TextFieldValue()
                        showClearButton = false
                    }) {
                        Icon(
                            modifier = Modifier.height(35.dp),
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.content_description_clear_content)
                        )
                    }
                }
            }
        },
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, textAlign = TextAlign.Start),
        placeholderTextStyle = LocalTextStyle.current.copy(fontSize = 14.sp, textAlign = TextAlign.Center),
        placeholderText = placeholderText,
        maxLines = 1,
        singleLine = true,
    )
}

@Preview(showBackground = true)
@Composable
fun SearchBarCollapsedPreview() {
    SearchBarUI("Search text")
}
