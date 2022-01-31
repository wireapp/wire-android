package com.wire.android.ui.login

import android.widget.Toast
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.BackNavigationIconButton
import com.wire.android.ui.theme.wireTypography

@Composable
fun LoginTopBar() {
    val context = LocalContext.current
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.wireTypography.title01
            )
        },
        navigationIcon = {
            BackNavigationIconButton {
                Toast.makeText(context, "Back click 💥", Toast.LENGTH_SHORT).show()  //TODO
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Preview(showBackground = false)
@Composable
fun LoginTopBarPreview() {
    LoginTopBar()
}
