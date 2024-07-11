/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.calling

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.wire.android.appLogger
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.notification.CallNotificationManager
import com.wire.android.ui.AppLockActivity
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.incoming.IncomingCallScreen
import com.wire.android.ui.calling.ongoing.OngoingCallScreen
import com.wire.android.ui.calling.outgoing.OutgoingCallScreen
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


fun startProjection(context: Context) {
    val mProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager?

    if (mProjectionManager != null) {
        ActivityCompat.startActivityForResult(
            context as Activity,
            mProjectionManager.createScreenCaptureIntent(),
            100,
            null
        )
    }
}

@AndroidEntryPoint
class CallActivity : AppCompatActivity() {

    @Inject
    lateinit var callNotificationManager: CallNotificationManager

    @Inject
    lateinit var proximitySensorManager: ProximitySensorManager

    val callActivityViewModel: CallActivityViewModel by viewModels()

    private val qualifiedIdMapper = QualifiedIdMapperImpl(null)

    private var mResultCode = 0
    private var mResultData: Intent? = null


    private val STATE_RESULT_CODE = "result_code"
    private val STATE_RESULT_DATA = "result_data"
    private val REQUEST_MEDIA_PROJECTION = 1

    var mSurface: Surface? = null
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaProjectionManager: MediaProjectionManager? = null
    var mSurfaceView: SurfaceView? = null

    private val activityReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            appLogger.d("$TAG -> received on receiver??")
             setUpMediaProjection()
             setUpVirtualDisplay()
        }
    }

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Create an intent filter to listen to the broadcast sent with the action "ACTION_STRING_ACTIVITY"
        val intentFilter = IntentFilter(BROADCAST_ACTION_START_SCREENSHARING)
        //Map the intent filter to the receiver
        registerReceiver(activityReceiver, intentFilter, RECEIVER_NOT_EXPORTED);

        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
            mResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
        }
//        setUpScreenShootPreventionFlag()
        setUpCallingFlags()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val conversationId = intent.extras?.getString(EXTRA_CONVERSATION_ID)
        val screenType = intent.extras?.getString(EXTRA_SCREEN_TYPE)
        val userId = intent.extras?.getString(EXTRA_USER_ID)

        userId?.let {
            qualifiedIdMapper.fromStringToQualifiedID(it).run {
                callActivityViewModel.switchAccountIfNeeded(this)
            }
        }
        mMediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        setUpCallingFlags()
//        setUpScreenShootPreventionFlag()

        appLogger.i("$TAG Initializing proximity sensor..")
        proximitySensorManager.initialize()

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalActivity provides this
            ) {
                WireTheme {
                    var currentCallScreenType by remember { mutableStateOf(screenType) }
                    currentCallScreenType?.let { currentScreenType ->
                        AnimatedContent(
                            targetState = currentScreenType,
                            transitionSpec = {
                                TransitionAnimationType.POP_UP.enterTransition.togetherWith(
                                    TransitionAnimationType.POP_UP.exitTransition
                                )
                            },
                            label = currentScreenType
                        ) { screenType ->
                            conversationId?.let {
                                when (screenType) {
                                    CallScreenType.Outgoing.name -> {
                                        OutgoingCallScreen(
                                            conversationId = qualifiedIdMapper.fromStringToQualifiedID(
                                                it
                                            )
                                        ) {
                                            currentCallScreenType = CallScreenType.Ongoing.name
                                        }
                                    }

                                    CallScreenType.Ongoing.name -> OngoingCallScreen(
                                        conversationId = qualifiedIdMapper.fromStringToQualifiedID(
                                            it
                                        ),
                                    )

                                    CallScreenType.Incoming.name -> IncomingCallScreen(
                                        qualifiedIdMapper.fromStringToQualifiedID(it)
                                    ) {
                                        currentCallScreenType = CallScreenType.Ongoing.name
                                    }
                                }
                            }
                        }
                    } ?: run { finish() }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        proximitySensorManager.registerListener()
    }

    override fun onPause() {
        super.onPause()
        proximitySensorManager.unRegisterListener()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            "MainActivity",
            "onActivityResult: requestCode=$requestCode, resultCode=$resultCode, data:$data"
        )
//        if (requestCode == MainActivity.REQUEST_CODE) {
//            if (resultCode == RESULT_OK) {
//                startService(getStartIntent(this, resultCode, data))
//            }
//        }
        mResultCode = resultCode
        mResultData = data
        if (requestCode == 100 && resultCode == -1) {
            startService(getStartIntent(this, resultCode, data))
        }

        Log.d(
            "MainActivity",
            "onActivityResult: at the end now???"
        )
    }

    private fun setUpMediaProjection() {
        mMediaProjection =
            mResultData?.let {
                Log.d("MainActivity", "setUpMediaProjection: setting the media projection from manager")
                mMediaProjectionManager!!.getMediaProjection(mResultCode, it)
            }
    }


    private fun tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
    }

    fun startScreenCapture() {
        Log.d(TAG, "startScreenCapture: ")
        if (mSurface == null) {
            return
        }
        if (mMediaProjection != null) {
            setUpVirtualDisplay()
        } else if (mResultCode != 0 && mResultData != null) {
            setUpMediaProjection()
            setUpVirtualDisplay()
        } else {
            Log.i(TAG, "Requesting confirmation")
            // This initiates a prompt dialog for the user to confirm screen projection.
            mMediaProjectionManager?.let {
                Log.i(TAG, "Requesting startActivityForResult")
                ActivityCompat.startActivityForResult(
                    this,
                    it.createScreenCaptureIntent(),
                    100,
                    null
                )
            }
        }
    }

    private fun setUpVirtualDisplay() {
        Log.d(TAG, "setUpVirtualDisplay: ")
        mMediaProjection?.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                }

                override fun onCapturedContentResize(width: Int, height: Int) {
                    super.onCapturedContentResize(width, height)
                }

                override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
                    super.onCapturedContentVisibilityChanged(isVisible)
                }
            },
            null,
        )
        mMediaProjection?.let { mmp ->
            val width = mSurfaceView?.width ?: 600
            val height = mSurfaceView?.height ?: 600
            Log.d(TAG, "setUpVirtualDisplay: MMP -> width: $width | height: $height")

            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)

            mSurfaceView?.let {
                Log.d(TAG, "setUpVirtualDisplay: createVirtualDisplay ")
                mVirtualDisplay = mmp.createVirtualDisplay(
                    "ScreenCapture2",
                    width, height, metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mSurface,
                    object : VirtualDisplay.Callback() {
                    }, null
                )
            }


            // val pPlatformView = PlatformView(mSurfaceView)
            // coreLogic.getSessionScope(userId!!).calls.setVideoPreview(ConversationId("", ""), PlatformView(mVirtualDisplay?.surface!!))
        }

    }

    override fun onDestroy() {
        stopScreenCapture()
        unregisterReceiver(activityReceiver)
        super.onDestroy()
    }

    private fun stopScreenCapture() {
        if (mVirtualDisplay == null) {
            return
        }
        mVirtualDisplay?.release()
        mVirtualDisplay = null
    }

    companion object {
        private const val TAG = "CallActivity"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_SCREEN_TYPE = "screen_type"

        const val BROADCAST_ACTION_START_SCREENSHARING = "BROADCAST_ACTION_START_SCREENSHARING"
    }
}

fun CallActivity.setUpCallingFlags() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    } else {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
        )
    }
}

fun CallActivity.setUpScreenShootPreventionFlag() {
    lifecycleScope.launch {
        if (callActivityViewModel.isScreenshotCensoringConfigEnabled().await()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

fun getOngoingCallIntent(
    activity: Activity,
    conversationId: String
) = Intent(activity, CallActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    putExtra(CallActivity.EXTRA_CONVERSATION_ID, conversationId)
    putExtra(CallActivity.EXTRA_SCREEN_TYPE, CallScreenType.Ongoing.name)
}

fun getOutgoingCallIntent(
    activity: Activity,
    conversationId: String
) = Intent(activity, CallActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    putExtra(CallActivity.EXTRA_CONVERSATION_ID, conversationId)
    putExtra(CallActivity.EXTRA_SCREEN_TYPE, CallScreenType.Outgoing.name)
}

fun getIncomingCallIntent(context: Context, conversationId: String, userId: String?) =
    Intent(context.applicationContext, CallActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(CallActivity.EXTRA_USER_ID, userId)
        putExtra(CallActivity.EXTRA_CONVERSATION_ID, conversationId)
        putExtra(CallActivity.EXTRA_SCREEN_TYPE, CallScreenType.Incoming.name)
    }

fun CallActivity.openAppLockActivity() {
    Intent(this, AppLockActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    }.run {
        startActivity(this)
    }
}
