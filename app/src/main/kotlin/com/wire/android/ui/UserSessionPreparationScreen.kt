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
package com.wire.android.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.Logo
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.logic.UserSessionPreparationFailure
import com.wire.kalium.logic.UserSessionPreparationState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun UserSessionPreparationScreen(
    state: UserSessionPreparationUiState,
    onRetry: () -> Unit,
    onUpdate: () -> Unit,
    onContactSupport: () -> Unit,
) {
    val content = state.content()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorsScheme().background)
            .padding(horizontal = dimensions().spacing32x),
    ) {
        when {
            state == UserSessionPreparationUiState.MigratingDatabase -> MigrationContent()
            content.action == null -> SplashContinuationContent(content)
            else -> {
                PreparationFailureContent(
                    content = content,
                    onRetry = onRetry,
                    onUpdate = onUpdate,
                    onContactSupport = onContactSupport,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.MigrationContent() {
    var phase by remember { mutableStateOf(MigrationScreenPhase.Updating) }
    LaunchedEffect(Unit) {
        delay(MIGRATION_LONG_RUNNING_MESSAGE_DELAY.inWholeMilliseconds)
        phase = migrationScreenPhase(MIGRATION_LONG_RUNNING_MESSAGE_DELAY)
    }

    MigrationContent(phase)
}

@Composable
private fun BoxScope.MigrationContent(phase: MigrationScreenPhase) {
    Logo(
        tint = colorsScheme().onBackground,
        modifier = Modifier
            .size(width = MIGRATION_CONTENT_WIDTH, height = MIGRATION_LOGO_HEIGHT)
            .align(Alignment.Center),
    )
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = SPLASH_COPY_OFFSET),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(phase.message),
            color = colorsScheme().onBackground,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(dimensions().spacing20x))
        LinearProgressIndicator(
            modifier = Modifier.width(MIGRATION_CONTENT_WIDTH),
            color = colorsScheme().primary,
            trackColor = colorsScheme().primaryVariant,
            strokeCap = StrokeCap.Butt,
        )
    }
}

/**
 * Continues the system splash with its final, static logo frame. Keeping the logo centred means
 * dismissing the system-owned splash does not look like a navigation event; only the explanatory
 * copy below it becomes visible.
 */
@Composable
private fun BoxScope.SplashContinuationContent(content: UserSessionPreparationContent) {
    Logo(
        tint = colorsScheme().onSurface,
        modifier = Modifier
            .size(width = SPLASH_LOGO_WIDTH, height = SPLASH_LOGO_HEIGHT)
            .align(Alignment.Center),
    )
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = SPLASH_COPY_OFFSET),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(content.title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(dimensions().spacing12x))
        Text(
            text = stringResource(content.message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BoxScope.PreparationFailureContent(
    content: UserSessionPreparationContent,
    onRetry: () -> Unit,
    onUpdate: () -> Unit,
    onContactSupport: () -> Unit,
) {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(content.title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(dimensions().spacing12x))
        Text(
            text = stringResource(content.message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        content.action?.let { action ->
            Spacer(Modifier.height(dimensions().spacing32x))
            Button(
                onClick = when (action) {
                    UserSessionPreparationAction.Retry -> onRetry
                    UserSessionPreparationAction.Update -> onUpdate
                    UserSessionPreparationAction.ContactSupport -> onContactSupport
                }
            ) {
                Text(stringResource(action.label))
            }
        }
    }
}

private val MIGRATION_CONTENT_WIDTH = 212.dp
private val MIGRATION_LOGO_HEIGHT = 67.dp

// The system splash uses a 288 dp icon canvas. Its Wire wordmark occupies roughly 174 x 55 dp.
private val SPLASH_LOGO_WIDTH = 174.dp
private val SPLASH_LOGO_HEIGHT = 55.dp
private val SPLASH_COPY_OFFSET = 96.dp

private class MigrationScreenPhasePreviewProvider : PreviewParameterProvider<MigrationScreenPhase> {
    override val values: Sequence<MigrationScreenPhase> = sequenceOf(
        MigrationScreenPhase.Updating,
        MigrationScreenPhase.StillUpdating,
    )
}

@Preview(
    name = "Active migration · Dark",
    widthDp = 360,
    heightDp = 800,
    uiMode = UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
private fun PreviewUserSessionPreparationScreen(
    @PreviewParameter(MigrationScreenPhasePreviewProvider::class)
    phase: MigrationScreenPhase,
) {
    WireTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorsScheme().background)
                .padding(horizontal = dimensions().spacing32x),
        ) {
            MigrationContent(phase)
        }
    }
}

internal sealed interface UserSessionPreparationUiState {
    data object ResolvingSession : UserSessionPreparationUiState
    data object OpeningDatabase : UserSessionPreparationUiState
    data object MigratingDatabase : UserSessionPreparationUiState
    data object Ready : UserSessionPreparationUiState
    data class Failed(val failure: UserSessionPreparationUiFailure) : UserSessionPreparationUiState
}

/**
 * Keeps fast session opening behind the system splash. A migration earns dedicated UI only when
 * it lasts long enough to avoid a flash; failures are revealed immediately so they stay actionable.
 *
 * Returning `null` means the state never justifies leaving the splash on its own.
 */
internal fun UserSessionPreparationUiState.preparationScreenRevealDelay(): Duration? = when (this) {
    UserSessionPreparationUiState.MigratingDatabase -> MIGRATION_SCREEN_REVEAL_DELAY
    is UserSessionPreparationUiState.Failed -> Duration.ZERO
    UserSessionPreparationUiState.ResolvingSession,
    UserSessionPreparationUiState.OpeningDatabase,
    UserSessionPreparationUiState.Ready -> null
}

/**
 * Tracks how long the migration screen has been visible so it is never replaced mid-blink.
 *
 * The reveal delay already skips the screen entirely for migrations that finish quickly. Once the
 * screen does appear, a migration finishing right after would swap it out instantly, which reads as
 * a glitch, so callers wait out [remainingVisibility] before showing the next screen.
 */
internal class MigrationScreenVisibility(
    private val minimumVisibility: Duration = MIGRATION_SCREEN_MINIMUM_VISIBILITY,
    private val elapsedRealtimeMillis: () -> Long,
) {
    private var revealedAtMillis: Long? = null

    fun onRevealed() {
        if (revealedAtMillis == null) revealedAtMillis = elapsedRealtimeMillis()
    }

    fun remainingVisibility(): Duration {
        val revealedAt = revealedAtMillis ?: return Duration.ZERO
        val visibleFor = (elapsedRealtimeMillis() - revealedAt).milliseconds
        return (minimumVisibility - visibleFor).coerceAtLeast(Duration.ZERO)
    }
}

internal enum class MigrationScreenPhase {
    Updating,
    StillUpdating,
}

internal fun migrationScreenPhase(elapsed: Duration): MigrationScreenPhase =
    if (elapsed >= MIGRATION_LONG_RUNNING_MESSAGE_DELAY) {
        MigrationScreenPhase.StillUpdating
    } else {
        MigrationScreenPhase.Updating
    }

internal enum class UserSessionPreparationUiFailure {
    InsufficientStorage,
    TemporarilyUnavailable,
    ApplicationUpdateRequired,
    SupportRequired,
}

/**
 * Maps Kalium's preparation states for a collector that cannot keep up with them.
 *
 * Kalium publishes on a conflated `StateFlow`, and the main thread is busy with the first frame
 * during startup. Reading the states straight from the main thread lets a short
 * [UserSessionPreparationState.MigratingDatabase] window be overwritten before it is ever seen, so
 * the migration screen never gets a chance to appear. Buffering hands the collector every state in
 * order instead, at the cost of observing them slightly later than they happened.
 */
internal fun Flow<UserSessionPreparationState>.toUiStates(): Flow<UserSessionPreparationUiState> =
    map { it.toUiState() }.buffer(Channel.UNLIMITED)

internal fun UserSessionPreparationState.toUiState(): UserSessionPreparationUiState = when (this) {
    UserSessionPreparationState.NotStarted -> UserSessionPreparationUiState.ResolvingSession
    UserSessionPreparationState.OpeningDatabase -> UserSessionPreparationUiState.OpeningDatabase
    UserSessionPreparationState.MigratingDatabase -> UserSessionPreparationUiState.MigratingDatabase
    UserSessionPreparationState.Ready -> UserSessionPreparationUiState.Ready
    is UserSessionPreparationState.Failed -> UserSessionPreparationUiState.Failed(failure.toUiFailure())
}

internal fun UserSessionPreparationFailure.toUiFailure(): UserSessionPreparationUiFailure = when (this) {
    is UserSessionPreparationFailure.InsufficientStorage -> UserSessionPreparationUiFailure.InsufficientStorage
    is UserSessionPreparationFailure.TemporarilyUnavailable -> UserSessionPreparationUiFailure.TemporarilyUnavailable
    is UserSessionPreparationFailure.ApplicationUpdateRequired -> UserSessionPreparationUiFailure.ApplicationUpdateRequired
    is UserSessionPreparationFailure.SupportRequired -> UserSessionPreparationUiFailure.SupportRequired
}

private enum class UserSessionPreparationAction(val label: Int) {
    Retry(R.string.label_try_again),
    Update(R.string.label_update),
    ContactSupport(R.string.user_session_preparation_contact_support),
}

private data class UserSessionPreparationContent(
    val title: Int,
    val message: Int,
    val action: UserSessionPreparationAction? = null,
)

/** How long a migration has to run before it is worth interrupting the splash for it. */
internal val MIGRATION_SCREEN_REVEAL_DELAY: Duration = 500.milliseconds

/** How long the migration screen stays up once revealed, even if the migration already finished. */
internal val MIGRATION_SCREEN_MINIMUM_VISIBILITY: Duration = 1.seconds

/** How long the initial migration copy is shown before reassuring the user that work is continuing. */
internal val MIGRATION_LONG_RUNNING_MESSAGE_DELAY: Duration = 10.seconds

private val MigrationScreenPhase.message: Int
    get() = when (this) {
        MigrationScreenPhase.Updating -> R.string.user_session_preparation_migrating_message
        MigrationScreenPhase.StillUpdating -> R.string.user_session_preparation_migrating_long_running_message
    }

@Composable
private fun UserSessionPreparationUiState.content(): UserSessionPreparationContent = when (this) {
    UserSessionPreparationUiState.ResolvingSession,
    UserSessionPreparationUiState.OpeningDatabase,
    UserSessionPreparationUiState.Ready -> UserSessionPreparationContent(
        title = R.string.user_session_preparation_opening_title,
        message = R.string.user_session_preparation_opening_message,
    )

    UserSessionPreparationUiState.MigratingDatabase -> UserSessionPreparationContent(
        title = R.string.user_session_preparation_migrating_title,
        message = R.string.user_session_preparation_migrating_message,
    )

    is UserSessionPreparationUiState.Failed -> when (failure) {
        UserSessionPreparationUiFailure.InsufficientStorage -> UserSessionPreparationContent(
            title = R.string.user_session_preparation_storage_title,
            message = R.string.user_session_preparation_storage_message,
            action = UserSessionPreparationAction.Retry,
        )

        UserSessionPreparationUiFailure.TemporarilyUnavailable -> UserSessionPreparationContent(
            title = R.string.user_session_preparation_temporary_title,
            message = R.string.user_session_preparation_temporary_message,
            action = UserSessionPreparationAction.Retry,
        )

        UserSessionPreparationUiFailure.ApplicationUpdateRequired -> UserSessionPreparationContent(
            title = R.string.update_app_dialog_title,
            message = R.string.update_app_dialog_body,
            action = UserSessionPreparationAction.Update,
        )

        UserSessionPreparationUiFailure.SupportRequired -> UserSessionPreparationContent(
            title = R.string.user_session_preparation_support_title,
            message = R.string.user_session_preparation_support_message,
            action = UserSessionPreparationAction.ContactSupport,
        )
    }
}
