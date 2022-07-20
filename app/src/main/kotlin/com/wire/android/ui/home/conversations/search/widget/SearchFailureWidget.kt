package com.wire.android.ui.home.conversations.search.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun SearchFailureBox(@StringRes failureMessage: Int){
    Box(
        Modifier
            .fillMaxWidth()
            .height(224.dp)
    ) {
        Text(
            stringResource(id = failureMessage),
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.wireTypography.label04.copy(color = MaterialTheme.wireColorScheme.secondaryText)
        )
    }
}
