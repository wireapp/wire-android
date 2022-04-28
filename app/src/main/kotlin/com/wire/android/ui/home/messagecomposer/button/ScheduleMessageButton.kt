package com.wire.android.ui.home.messagecomposer.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R

@Composable
fun ScheduleMessageButton() {
    IconButton(onClick = { }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_timer),
            contentDescription = stringResource(R.string.content_description_back_button),
        )
    }
}
