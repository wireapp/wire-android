package com.wire.android.ui.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import com.wire.android.ui.common.button.wireRadioButtonColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireRadioButton(
    checked: Boolean,
    onButtonChecked: (() -> Unit)
) {
    RadioButton(
        selected = checked,
        onClick = onButtonChecked,
        colors = wireRadioButtonColors()
    )
}
