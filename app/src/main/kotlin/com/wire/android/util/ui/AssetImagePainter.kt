package com.wire.android.util.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.wire.android.R
import com.wire.android.model.UserAvatarAsset
import com.wire.android.util.getUriFromDrawable

@Composable
fun assetImagePainter(asset: UserAvatarAsset?) = rememberAsyncImagePainter(
    ImageRequest.Builder(LocalContext.current)
        .data(
            asset ?: getUriFromDrawable(
                LocalContext.current,
                R.drawable.ic_default_user_avatar
            )
        ).build()
)
