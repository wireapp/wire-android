/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.debug.featureflags

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import com.wire.android.model.Clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.common.rowitem.RowItem

@Composable
fun FeatureListItem(
    feature: Feature,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (feature.status == FeatureStatus.ENABLED) 1f else 0.5f),
    ) {
        RowItem(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = dimensions().spacing48x)
                .padding(dimensions().spacing16x),
            clickable = Clickable {
                if (feature.configJson != null) {
                    expanded = !expanded
                } else {
                    showMessage(
                        context = context,
                        message = "No configuration available.",
                    )
                }
            }
        ) {
            Text(
                text = feature.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                style = typography().body01,
                color = colorsScheme().onBackground,
            )

            when (feature.status) {
                FeatureStatus.ENABLED -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Enabled",
                    tint = colorsScheme().primary
                )

                FeatureStatus.DISABLED -> Icon(
                    imageVector = Icons.Default.DoNotDisturb,
                    contentDescription = "Disabled",
                    tint = colorsScheme().onSurfaceVariant,
                )

                FeatureStatus.NOT_CONFIGURED -> {}
            }
        }

        feature.configJson?.let { config ->
            AnimatedVisibility(expanded) {
                Text(
                    text = config,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensions().spacing16x),
                    style = typography().label01,
                    color = colorsScheme().secondaryText,
                )
            }
        }
    }
}

private fun showMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
