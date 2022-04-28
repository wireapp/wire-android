package com.wire.android.ui.home.messagecomposer.button

import androidx.compose.runtime.Composable
import com.wire.android.R
import com.wire.android.ui.common.button.WireIconButton

@Composable
 fun AddMentionAction() {
    WireIconButton(
        onButtonClicked = {},
        iconResource = R.drawable.ic_mention,
        contentDescription = R.string.content_description_conversation_search_icon
    )
}
