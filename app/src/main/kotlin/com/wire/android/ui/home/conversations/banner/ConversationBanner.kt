package com.wire.android.ui.home.conversations.banner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.StatusLabel
import com.wire.android.util.ui.UIText

@Composable
fun ConversationBanner(bannerMessage: UIText?) {
    bannerMessage?.let {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.primary)
                .padding(vertical = dimensions().spacing6x, horizontal = dimensions().spacing16x),
            contentAlignment = Alignment.Center
        ) {
            StatusLabel(it.asString())
        }
    }
}

@Preview
@Composable
fun ConversationBannerPreview() {
    ConversationBanner(bannerMessage = UIText.DynamicString("Federated users, Externals, guests and services are present"))
}
