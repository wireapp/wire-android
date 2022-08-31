package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.feature.conversation.ClassifiedType

@Composable
fun ClassifiedIndicatorBanner(
    classifiedType: ClassifiedType,
    modifier: Modifier = Modifier,
) {
    if (classifiedType != ClassifiedType.NONE) {
        Divider()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .background(getBackgroundColorFor(classifiedType))
                .height(dimensions().spacing24x)
                .fillMaxWidth()
        ) {
            Text(
                text = getTextFor(classifiedType),
                color = getColorTextFor(classifiedType),
                style = MaterialTheme.wireTypography.label03
            )
        }
        Divider()
    }
}

@Composable
private fun getTextFor(classifiedType: ClassifiedType): String {
    return if (classifiedType == ClassifiedType.CLASSIFIED) {
        stringResource(id = R.string.conversation_details_is_classified)
    } else {
        stringResource(id = R.string.conversation_details_is_not_classified)
    }
}

@Composable
private fun getBackgroundColorFor(classifiedType: ClassifiedType): Color {
    return if (classifiedType == ClassifiedType.CLASSIFIED) {
        colorsScheme().surface
    } else {
        colorsScheme().onSurface
    }
}

@Composable
private fun getColorTextFor(classifiedType: ClassifiedType): Color {
    return if (classifiedType == ClassifiedType.CLASSIFIED) {
        colorsScheme().onSurface
    } else {
        colorsScheme().surface
    }
}

@Preview(showBackground = true)
@Composable
fun ClassifiedIndicatorPreview() {
    Column(modifier = Modifier.fillMaxWidth()) {
        ClassifiedIndicatorBanner(classifiedType = ClassifiedType.CLASSIFIED)

        Divider()

        ClassifiedIndicatorBanner(classifiedType = ClassifiedType.NOT_CLASSIFIED)
    }

}
