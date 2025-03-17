package com.wire.android.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun WirePromotionDialog(
    title: String,
    description: String,
    buttonLabel: String,
    onDismiss: () -> Unit,
    onCreateTeam: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.dialogCornerSize),
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.wireDimensions.dialogContentPadding)
) {

    Dialog(
        onDismissRequest = onDismiss,
        properties = properties
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(MaterialTheme.wireDimensions.dialogCardMargin),
            shape = shape,
            border = BorderStroke(1.dp, colorsScheme().secondaryButtonEnabledPromotion),
            color = Color.Black,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(id = R.drawable.ic_wave),
                contentDescription = null,
                alignment = Alignment.TopEnd,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.wireTypography.title02,
                    color = Color.White,
                    modifier = Modifier.padding(
                        end = dimensions().spacing64x,
                        bottom = MaterialTheme.wireDimensions.dialogTextsSpacing
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.wireTypography.body01,
                    color = Color.White,
                    modifier = Modifier.padding(
                        end = dimensions().spacing100x,
                        bottom = MaterialTheme.wireDimensions.dialogTextsSpacing
                    )
                )
                WireSecondaryButton(
                    modifier = Modifier,
                    text = buttonLabel,
                    onClick = onCreateTeam,
                    fillMaxWidth = false,
                    colors = wireSecondaryButtonColors().copy(
                        onEnabled = colorsScheme().onSecondaryButtonEnabledPromotion,
                        enabled = colorsScheme().secondaryButtonEnabledPromotion,
                        enabledOutline = colorsScheme().secondaryButtonEnabledOutlinePromotion,
                        enabledRipple = colorsScheme().secondaryButtonRipplePromotion,
                        positive = MaterialTheme.wireColorScheme.secondaryButtonEnabledPromotion,
                        positiveOutline = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutlinePromotion,
                        positiveRipple = MaterialTheme.wireColorScheme.secondaryButtonRipplePromotion,
                    ),
                    minSize = dimensions().buttonSmallMinSize,
                    minClickableSize = dimensions().buttonMinClickableSize,
                )
            }
        }

    }
}

@MultipleThemePreviews
@Composable
fun PreviewWireDialogWithWaves() {
    WirePromotionDialog(
        title = "Title",
        description = "Description",
        buttonLabel = "Button",
        onDismiss = {},
        onCreateTeam = {},
    )
}
