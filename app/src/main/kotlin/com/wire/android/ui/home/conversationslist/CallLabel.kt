package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wire.android.ui.home.conversationslist.model.CallEvent
import com.wire.android.ui.home.conversationslist.model.CallInfo
import com.wire.android.ui.home.conversationslist.model.CallTime
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun CallLabel(callInfo: CallInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TimeLabel(callTime = callInfo.callTime)
        Spacer(modifier = Modifier.width(6.dp))
        CallEventIcon(callEvent = callInfo.callEvent)
    }
}

@Composable
private fun CallEventIcon(callEvent: CallEvent, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = callEvent.drawableResourceId),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
private fun TimeLabel(callTime: CallTime) {
    Text(text = callTime.toLabel(), style = MaterialTheme.wireTypography.subline01, color = MaterialTheme.wireColorScheme.secondaryText)
}
