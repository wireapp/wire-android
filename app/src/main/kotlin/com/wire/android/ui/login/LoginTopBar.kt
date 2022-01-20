package com.wire.android.ui.login

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.ui.theme.WireLightColors

@Composable
fun LoginTopBar() {
    val context = LocalContext.current
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.login_title),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth().padding(end = 68.dp) // same as AppBar TitleIconModifier width
            )
        },
        elevation = 0.dp,
        navigationIcon = {
            IconButton(
                onClick = { Toast.makeText(context, "Back click 💥", Toast.LENGTH_SHORT).show() }, //TODO
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "",
                    tint = WireLightColors.onBackground
                )
            }
        },
        backgroundColor = WireLightColors.background,
        contentColor = WireLightColors.onBackground
    )
}

@Preview(showBackground = false)
@Composable
fun LoginTopBarPreview() {
    LoginTopBar()
}
