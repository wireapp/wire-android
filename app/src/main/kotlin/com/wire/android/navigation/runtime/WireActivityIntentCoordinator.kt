/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.navigation.runtime

import android.content.Intent
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Owns the Activity-level intent queue and the Android saved-state protocol used to prevent
 * a deep link from being handled again after Activity recreation.
 *
 * The queue is intentionally unlimited: intents may arrive before the Compose navigation host
 * starts collecting, and every intent must be delivered exactly once in arrival order.
 */
internal class WireActivityIntentCoordinator {

    private val pendingRequests = Channel<WireActivityIntentRequest>(Channel.UNLIMITED)

    val requests: Flow<WireActivityIntentRequest> = pendingRequests.receiveAsFlow()

    fun enqueue(intent: Intent?, savedInstanceState: Bundle? = null) {
        enqueue(WireActivityIntentRequest(intent, savedInstanceState))
    }

    fun enqueue(request: WireActivityIntentRequest) {
        check(pendingRequests.trySend(request).isSuccess) {
            "WireActivity intent queue is unexpectedly closed"
        }
    }

    fun saveInstanceState(outState: Bundle, currentIntent: Intent?) {
        outState.putBoolean(HANDLED_DEEP_LINK_KEY, true)
        outState.putParcelable(ORIGINAL_INTENT_KEY, currentIntent)
    }

    fun restoreActivityIntent(savedInstanceState: Bundle): Intent? =
        savedInstanceState.originalIntent()

    suspend fun handle(
        request: WireActivityIntentRequest,
        isEmptyWelcomeStartDestination: () -> Boolean,
        handleNonDeepLinkIntent: suspend (Intent?) -> Boolean,
        handleDeepLink: suspend (Intent) -> Unit,
    ): WireActivityIntentEffect {
        val intent = request.intent
        val handling = decideWireActivityIntentHandling(
            WireActivityIntentEvidence(
                isMissing = intent == null,
                isLauncherIntent = intent?.action == Intent.ACTION_MAIN,
                isLaunchedFromHistory = intent?.let {
                    it.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
                } == true,
                isRestoredOriginalIntent = request.savedInstanceState.originalIntent() == intent,
                wasDeepLinkHandled = intent?.getBooleanExtra(HANDLED_DEEP_LINK_KEY, false) == true,
            )
        )
        val wasHandledAsNonDeepLink = handleNonDeepLinkIntent(intent)

        if (handling == WireActivityIntentHandling.NON_DEEP_LINK_THEN_DEEP_LINK &&
            !wasHandledAsNonDeepLink &&
            intent != null
        ) {
            handleDeepLink(intent)
            intent.putExtra(HANDLED_DEEP_LINK_KEY, true)
        }

        return if (
            handling == WireActivityIntentHandling.NON_DEEP_LINK_ONLY &&
            !wasHandledAsNonDeepLink &&
            isEmptyWelcomeStartDestination()
        ) {
            WireActivityIntentEffect.OPEN_LOGIN
        } else {
            WireActivityIntentEffect.NONE
        }
    }

    private fun Bundle?.originalIntent(): Intent? =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            this?.getParcelable(ORIGINAL_INTENT_KEY)
        } else {
            this?.getParcelable(ORIGINAL_INTENT_KEY, Intent::class.java)
        }

    private companion object {
        const val HANDLED_DEEP_LINK_KEY = "deeplink_handled_flag_key"
        const val ORIGINAL_INTENT_KEY = "original_saved_intent"
    }
}

internal data class WireActivityIntentRequest(
    val intent: Intent?,
    val savedInstanceState: Bundle?,
    val hasTrustedWireShareCaller: Boolean = false,
)

internal enum class WireActivityIntentEffect {
    NONE,
    OPEN_LOGIN,
}

internal enum class WireActivityIntentHandling {
    NON_DEEP_LINK_ONLY,
    NON_DEEP_LINK_THEN_DEEP_LINK,
}

internal data class WireActivityIntentEvidence(
    val isMissing: Boolean = false,
    val isLauncherIntent: Boolean = false,
    val isLaunchedFromHistory: Boolean = false,
    val isRestoredOriginalIntent: Boolean = false,
    val wasDeepLinkHandled: Boolean = false,
)

internal fun decideWireActivityIntentHandling(
    evidence: WireActivityIntentEvidence,
): WireActivityIntentHandling =
    if (evidence.shouldSkipDeepLinkHandling) {
        WireActivityIntentHandling.NON_DEEP_LINK_ONLY
    } else {
        WireActivityIntentHandling.NON_DEEP_LINK_THEN_DEEP_LINK
    }

private val WireActivityIntentEvidence.shouldSkipDeepLinkHandling: Boolean
    get() = isMissing ||
        isLauncherIntent ||
        isLaunchedFromHistory ||
        isRestoredOriginalIntent ||
        wasDeepLinkHandled
