package com.wire.android.ui.home

import android.content.Intent
import android.content.Intent.ACTION_SENDTO
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Email
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.navigation.HomeNavigationItem.Settings
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationItem.Support
import com.wire.android.navigation.isExternalRoute
import com.wire.android.ui.common.Logo
import com.wire.android.ui.common.selectableBackground
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.EmailComposer
import com.wire.android.util.getDeviceId
import com.wire.android.util.getUrisOfFilesInDirectory
import com.wire.android.util.multipleFileSharingIntent
import com.wire.android.util.sha256
import java.io.File
import java.util.Date

@ExperimentalMaterialApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
//TODO: logFilePath does not belong in the UI logic
fun HomeDrawer(
    logFilePath: String,
    currentRoute: String?,
    navigateToHomeItem: (HomeNavigationItem) -> Unit,
    navigateToItem: (NavigationItem) -> Unit,
    onCloseDrawer: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(
                start = MaterialTheme.wireDimensions.homeDrawerHorizontalPadding,
                end = MaterialTheme.wireDimensions.homeDrawerHorizontalPadding,
                bottom = MaterialTheme.wireDimensions.homeDrawerBottomPadding
            )

    ) {
        Logo()

        fun navigateAndCloseDrawer(item: Any) {
            when (item) {
                is HomeNavigationItem -> navigateToHomeItem(item)
                is NavigationItem -> when (item.isExternalRoute()) {
                    true -> CustomTabsHelper.launchUrl(context, item.getRouteWithArgs())
                    false -> navigateToItem(item)
                }
                else -> {}
            }
            onCloseDrawer()
        }

       val topItems = listOf(HomeNavigationItem.Conversations)
        // TODO: Re-enable once we have Archive & Vault
        // listOf(HomeNavigationItem.Conversations, HomeNavigationItem.Archive, HomeNavigationItem.Vault)

        topItems.forEach { item ->
            DrawerItem(
                data = item.getDrawerData(),
                selected = currentRoute == item.route(),
                onItemClick = remember { { navigateAndCloseDrawer(item) } }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        val bottomItems = buildList {
            add(Settings)
            add(Support)
        }

        bottomItems.forEach { item ->
            DrawerItem(
                data = item.getDrawerData(),
                selected = currentRoute == item.route(),
                onItemClick = remember { { navigateAndCloseDrawer(item) } }
            )
        }

        DrawerItem(
            data = DrawerItemData(R.string.give_feedback_screen_title, R.drawable.ic_emoticon),
            selected = false,
            onItemClick = {
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("wire-newandroid-feedback@wearezeta.zendesk.com"))
                intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback - Wire Beta")
                intent.putExtra(Intent.EXTRA_TEXT, EmailComposer.giveFeedbackEmailTemplate(context.getDeviceId()?.sha256()))

                intent.selector = Intent(ACTION_SENDTO).setData(Uri.parse("mailto:"))
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_feedback_choose_email)))
            })

        DrawerItem(
            data = DrawerItemData(R.string.report_bug_screen_title, R.drawable.ic_bug),
            selected = false,
            onItemClick = {
                val dir = File(logFilePath).parentFile

                if (dir != null) {
                val logsUris = context.getUrisOfFilesInDirectory(dir)
                val intent = context.multipleFileSharingIntent(logsUris)
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("wire-newandroid@wearezeta.zendesk.com"))
                intent.putExtra(Intent.EXTRA_SUBJECT, "Bug Report - Wire Beta")
                intent.putExtra(Intent.EXTRA_TEXT, EmailComposer.reportBugEmailTemplate(context.getDeviceId()?.sha256()))
                intent.type = "message/rfc822"

                context.startActivity(Intent.createChooser(intent, "Choose an Email client: "))
            }}
        )

        Text(
            text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(10.dp)
        )
    }
}

// TODO:Mateusz: this should not belong here nor in ViewModel
private fun reportBugEmailTemplate(deviceHash: String? = "unavailable"): String = """
        --- DO NOT EDIT---
        App Version: ${BuildConfig.VERSION_NAME}
        Device Hash: $deviceHash
        Device: ${Build.MANUFACTURER} - ${Build.MODEL}
        SDK: ${Build.VERSION.RELEASE}
        Date: ${Date()}
        ------------------

        Please fill in the following

        - Date & Time of when the issue occurred:


        - What happened:


        - Steps to reproduce (if relevant):
        
    """.trimIndent()

@Composable
fun DrawerItem(data: DrawerItemData, selected: Boolean, onItemClick: () -> Unit) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .fillMaxWidth()
            .height(40.dp)
            .background(backgroundColor)
            .selectableBackground(selected) { onItemClick() },
    ) {
        Image(
            painter = painterResource(id = data.icon!!),
            contentDescription = stringResource(data.title!!),
            colorFilter = ColorFilter.tint(contentColor),
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
        )
        Text(
            style = MaterialTheme.wireTypography.button02,
            text = stringResource(id = data.title),
            color = contentColor,
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )
    }
}

data class DrawerItemData(@StringRes val title: Int?, @DrawableRes val icon: Int?)

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
private fun Any.getDrawerData(): DrawerItemData =
    when (this) {
        is HomeNavigationItem -> DrawerItemData(this.title, this.icon)
        Support -> DrawerItemData(R.string.support_screen_title, R.drawable.ic_support)
        else -> DrawerItemData(null, null)
    }

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
fun Any.route() = when (this) {
    is HomeNavigationItem -> this.route
    is NavigationItem -> this.getRouteWithArgs()
    else -> null
}
