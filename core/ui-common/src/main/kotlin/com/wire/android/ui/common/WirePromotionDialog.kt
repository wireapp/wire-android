package com.wire.android.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireColorScheme
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions

@Composable
fun WirePromotionDialog(
    title: String,
    description: String,
    buttonLabel: String,
    onDismiss: () -> Unit,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = wireDialogPropertiesBuilder(),
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.dialogCornerSize),
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.wireDimensions.dialogContentPadding)
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = properties
    ) {
        WirePromotionCard(
            title = title,
            description = description,
            buttonLabel = buttonLabel,
            onButtonClick = onButtonClick,
            modifier = modifier,
            shape = shape,
            contentPadding = contentPadding
        )
    }
}

@Composable
fun WirePromotionCard(
    title: String,
    description: String,
    buttonLabel: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.dialogCornerSize),
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.wireDimensions.dialogContentPadding)
) {
    WireColorScheme(darkColorsScheme()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            border = BorderStroke(dimensions().spacing1x, colorsScheme().outline),
        ) {
            Box(modifier = Modifier.height(IntrinsicSize.Min)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = title,
                        style = typography().title02,
                        modifier = Modifier.padding(
                            end = dimensions().spacing64x,
                            bottom = dimensions().spacing8x
                        )
                    )
                    Text(
                        text = description,
                        style = typography().body01,
                        modifier = Modifier.padding(
                            end = dimensions().spacing100x,
                            bottom = dimensions().spacing16x
                        ),
                    )
                    WireSecondaryButton(
                        text = buttonLabel,
                        onClick = onButtonClick,
                        fillMaxWidth = false,
                        minSize = dimensions().buttonSmallMinSize,
                        minClickableSize = dimensions().buttonSmallMinSize,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bg_waves_promotion),
                        contentDescription = null,
                        alignment = Alignment.TopEnd,
                        contentScale = ContentScale.None,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewWireDialogWithWaves() = WireTheme {
    Box(modifier = Modifier.padding(dimensions().dialogCardMargin)) {
        WirePromotionDialog(
            title = "Title",
            description = "Description",
            buttonLabel = "Button",
            onDismiss = {},
            onButtonClick = {},
        )
    }
}
