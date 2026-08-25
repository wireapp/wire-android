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
package com.wire.android.ui.debug.experimental

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.WireSwitch
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.debug.debugExperimentalFeaturesViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.debug.ExperimentalFeature
import com.wire.android.util.ui.PreviewMultipleThemes

@WireRootDestination
@Composable
fun DebugExperimentalFeaturesScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: DebugExperimentalFeaturesViewModel = debugExperimentalFeaturesViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    DebugExperimentalFeaturesContent(
        state = state,
        modifier = modifier,
        onNavigationPressed = navigator::navigateBack,
        onFeatureCheckedChange = viewModel::setEnabled,
        onResetAll = viewModel::resetAll,
        onRestartApp = { restartApp(context) },
    )
}

@Composable
private fun DebugExperimentalFeaturesContent(
    state: DebugExperimentalFeaturesState,
    onNavigationPressed: () -> Unit,
    onFeatureCheckedChange: (key: String, enabled: Boolean) -> Unit,
    onResetAll: () -> Unit,
    onRestartApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.debug_settings_experimental_features),
                elevation = scrollState.rememberTopBarElevationState().value,
                navigationIconType = NavigationIconType.Back(),
                onNavigationPressed = onNavigationPressed,
            )
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(internalPadding)
        ) {
            Text(
                text = stringResource(R.string.debug_experimental_features_description),
                modifier = Modifier.padding(dimensions().spacing16x),
                style = typography().body01,
                color = colorsScheme().secondaryText,
            )

            AnimatedVisibility(state.isRestartRequired) {
                RestartRequiredBanner(onRestartApp = onRestartApp)
            }

            SectionHeader(stringResource(R.string.debug_settings_experimental_features))

            if (state.features.isEmpty()) {
                Text(
                    text = stringResource(R.string.debug_experimental_features_empty),
                    modifier = Modifier.padding(dimensions().spacing16x),
                    style = typography().body01,
                    color = colorsScheme().secondaryText,
                )
            } else {
                state.features.forEach { feature ->
                    ExperimentalFeatureItem(
                        feature = feature,
                        onCheckedChange = { enabled -> onFeatureCheckedChange(feature.key, enabled) },
                    )
                }

                WireSecondaryButton(
                    text = stringResource(R.string.debug_experimental_features_reset),
                    onClick = onResetAll,
                    modifier = Modifier.padding(dimensions().spacing16x),
                )
            }
        }
    }
}

@Composable
private fun ExperimentalFeatureItem(
    feature: ExperimentalFeature,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    RowItemTemplate(
        modifier = modifier.fillMaxWidth(),
        title = {
            Text(
                text = feature.name,
                style = typography().body01,
                color = colorsScheme().onBackground,
            )
        },
        subtitle = {
            Text(
                text = stringResource(
                    R.string.debug_experimental_features_item_subtitle,
                    feature.key,
                    feature.compiledValue.toString(),
                ),
                textAlign = TextAlign.Start,
                style = typography().label04,
                color = if (feature.isOverridden) colorsScheme().error else colorsScheme().secondaryText,
            )
        },
        actions = {
            WireSwitch(
                checked = feature.isEnabled,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(end = dimensions().spacing8x),
            )
        },
    )
}

@Composable
private fun RestartRequiredBanner(
    onRestartApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceBackgroundWrapper(modifier = modifier) {
        Column(Modifier.padding(dimensions().spacing16x)) {
            Text(
                text = stringResource(R.string.debug_experimental_features_restart_required),
                style = typography().body01,
                color = colorsScheme().error,
            )
            WirePrimaryButton(
                text = stringResource(R.string.debug_experimental_features_restart),
                onClick = onRestartApp,
                modifier = Modifier.padding(top = dimensions().spacing8x),
            )
        }
    }
}

/**
 * The flags are compile time constants read once while the app is starting up, so the process has
 * to be recreated for an override to take effect.
 */
private fun restartApp(context: Context) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    launchIntent?.component?.let { component ->
        context.startActivity(Intent.makeRestartActivityTask(component))
    }
    Runtime.getRuntime().exit(0)
}

@PreviewMultipleThemes
@Composable
fun PreviewDebugExperimentalFeaturesScreen() = WireTheme {
    DebugExperimentalFeaturesContent(
        state = DebugExperimentalFeaturesState(
            features = listOf(
                ExperimentalFeature(key = "feature_offline_files_enabled", compiledValue = true, override = null),
                ExperimentalFeature(key = "feature_in_app_image_viewer_enabled", compiledValue = false, override = true),
            ),
            isRestartRequired = true,
        ),
        onNavigationPressed = {},
        onFeatureCheckedChange = { _, _ -> },
        onResetAll = {},
        onRestartApp = {},
    )
}
