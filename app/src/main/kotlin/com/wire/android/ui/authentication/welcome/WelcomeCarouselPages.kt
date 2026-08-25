/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.welcome

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.authentication.R as AuthenticationR

@Composable
internal fun welcomeCarouselPages() = listOf(
    WelcomeCarouselPage(
        AuthenticationR.drawable.ic_welcome_1,
        stringResource(AuthenticationR.string.welcome_screen_carousel_item_message_1),
    ),
    WelcomeCarouselPage(
        AuthenticationR.drawable.ic_welcome_2,
        stringResource(AuthenticationR.string.welcome_screen_carousel_item_message_2),
    ),
    WelcomeCarouselPage(
        AuthenticationR.drawable.ic_welcome_3,
        stringResource(AuthenticationR.string.welcome_screen_carousel_item_message_3),
    ),
    WelcomeCarouselPage(
        AuthenticationR.drawable.ic_welcome_4,
        stringResource(AuthenticationR.string.welcome_screen_carousel_item_message_4),
    ),
    WelcomeCarouselPage(
        AuthenticationR.drawable.ic_welcome_5,
        stringResource(AuthenticationR.string.welcome_screen_carousel_item_message_5),
    ),
)
