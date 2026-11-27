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
package com.wire.android.ui.edit

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.wire.android.appLogger
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.ui.AppLockActivity
import com.wire.android.ui.BaseActivity
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.android.ui.theme.WireTheme
import dev.zacsweers.metro.Inject

/**
 * Shows the online document editor inside the app, below a native top bar, so the editor's own UI
 * (browser menu, share, copy link) is never exposed. Navigation is limited to the editor's own host.
 */
class OnlineEditorActivity : BaseActivity() {

    @Inject
    lateinit var lockCodeTimeManager: Lazy<LockCodeTimeManager>

    override fun onCreate(savedInstanceState: Bundle?) {
        wireApplicationGraph.inject(this)
        super.onCreate(savedInstanceState)

        val editorUrl = intent.dataString
        if (editorUrl == null) {
            finish()
            return
        }

        setContent {
            WireTheme {
                OnlineEditorScreen(editorUrl = editorUrl, onClose = ::finish)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (lockCodeTimeManager.value.isAppLocked()) {
            val userId = intent.getStringExtra(EXTRA_USER_ID)
            if (userId == null) {
                appLogger.e("OnlineEditor: app is locked but no user id, closing")
                finish()
                return
            }
            startActivity(
                Intent(this, AppLockActivity::class.java).apply { putExtra(AppLockActivity.EXTRA_USER_ID, userId) }
            )
        }
    }

    companion object {
        const val EXTRA_USER_ID = "user_id"
    }
}

@Composable
private fun OnlineEditorScreen(editorUrl: String, onClose: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else onClose()
    }

    WireScaffold(
        snackbarHost = {},
        topBar = {
            WireCenterAlignedTopAppBar(
                title = title,
                navigationIconType = NavigationIconType.Close(),
                onNavigationPressed = onClose,
            )
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
                .navigationBarsPadding()
        ) {
            if (progress in 0f..PROGRESS_DONE_THRESHOLD) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = colorsScheme().primary,
                )
            }
            EditorWebView(
                editorUrl = editorUrl,
                onClose = onClose,
                onTitle = { title = it },
                onProgress = { progress = it },
                onCreated = { webView = it },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EditorWebView(
    editorUrl: String,
    onClose: () -> Unit,
    onTitle: (String) -> Unit,
    onProgress: (Float) -> Unit,
    onCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editorHost = remember(editorUrl) { Uri.parse(editorUrl).host }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                }
                // No text selection menu or long-press actions, so the link and content can't be copied or shared.
                isLongClickable = false
                setOnLongClickListener { true }
                isHapticFeedbackEnabled = false

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (request.url.host == editorHost) return false
                        // Leaving the editor (e.g. its own close button) ends the screen, nothing opens elsewhere.
                        if (request.isForMainFrame) onClose()
                        return true
                    }

                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) = onProgress(0f)
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView, title: String?) = onTitle(title.orEmpty())
                    override fun onProgressChanged(view: WebView, newProgress: Int) = onProgress(newProgress / PERCENT)
                }

                onCreated(this)
                loadUrl(editorUrl)
            }
        },
        onRelease = { it.destroy() }
    )
}

private const val PERCENT = 100f
private const val PROGRESS_DONE_THRESHOLD = 0.99f
