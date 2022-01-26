package com.wire.android.ui.login

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.theme.title01

@Composable
fun LoginTopBar() {
    val context = LocalContext.current
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.title01
            )
        },
        navigationIcon = {
            IconButton(
                onClick = { Toast.makeText(context, "Back click 💥", Toast.LENGTH_SHORT).show() }, //TODO
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.onBackground
                )
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
