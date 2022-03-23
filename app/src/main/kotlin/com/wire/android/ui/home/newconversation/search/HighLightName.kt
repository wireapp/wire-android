package com.wire.android.ui.home.newconversation.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.MatchQueryResult
import com.wire.android.util.QueryMatchExtractor
import kotlinx.coroutines.launch

@Composable
fun HighLightName(
    name: String,
    searchQuery: String,
) {
    val scope = rememberCoroutineScope()

    var highlightIndexes by remember {
        mutableStateOf(emptyList<MatchQueryResult>())
    }

    SideEffect {
        scope.launch {
            highlightIndexes = QueryMatchExtractor.extractQueryMatchIndexes(
                matchText = searchQuery,
                text = name
            )
        }
    }

    if (highlightIndexes.isNotEmpty()) {
        Text(
            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = MaterialTheme.wireTypography.title02.fontWeight,
                        fontSize = MaterialTheme.wireTypography.title02.fontSize,
                        fontFamily = MaterialTheme.wireTypography.title02.fontFamily,
                        fontStyle = MaterialTheme.wireTypography.title02.fontStyle
                    )
                ) {
                    append(name)
                }

                highlightIndexes
                    .forEach { highLightIndexes ->
                        addStyle(
                            style = SpanStyle(background = MaterialTheme.wireColorScheme.highLight.copy(alpha = 0.5f)),
                            start = highLightIndexes.startIndex,
                            end = highLightIndexes.endIndex
                        )
                    }
            }
        )
    } else {
        Text(
            text = name,
            style = MaterialTheme.wireTypography.title02
        )
    }
}
