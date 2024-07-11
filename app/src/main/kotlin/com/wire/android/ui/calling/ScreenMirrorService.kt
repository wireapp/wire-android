package com.wire.android.ui.calling

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import android.view.OrientationEventListener
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import com.wire.android.BuildConfig
import com.wire.android.WireApplication
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.parcelable
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.util.PlatformView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class ScreenMirrorService : Service() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var currentSession: CurrentSessionUseCase

    private var widthPortrait = 720
    private var heightPortrait = 1280
    private var widthLandscape = 1280
    private var heightLandscape = 720
    private var mScreenDensity = 0
    private var mResultCode = 0
    private var mResultData: Intent? = null
    private var mBitmap: Bitmap? = null
    private lateinit var orientationEventListener: OrientationEventListener
    private var isPortrait = true

    private var mMediaProjection: MediaProjection? = null
    private var mImageReaderPortrait: ImageReader? = null
    private var mImageReaderLandscape: ImageReader? = null
    private var mImageReaderHandlerThread: HandlerThread? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var handler: Handler? = null

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.ensureDefaultChannel()
        isPortrait = true
        val metrics = resources.displayMetrics
        if (isPortrait) {
            widthPortrait = metrics.widthPixels
            heightPortrait = metrics.heightPixels
        } else {
            widthLandscape = metrics.heightPixels
            heightLandscape = metrics.widthPixels
        }
        mScreenDensity = metrics.densityDpi
        orientationEventListener =
            object : OrientationEventListener(this) {
                override fun onOrientationChanged(orientation: Int) {
                    val newIsPortrait = true
                    if (isPortrait != newIsPortrait) {
                        isPortrait = newIsPortrait
                        resize()
                    }
                }
            }
        val notification =
            NotificationHelper.createServiceNotification(
                this,
                "${BuildConfig.APPLICATION_ID}.action.stop_screen_mirror",
                "sharing screen..",
            )
        val id = NotificationHelper.generateId()
        ServiceCompat.startForeground(this, id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    }


    @SuppressLint("WrongConstant")
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Log.d("screenMirrorService", "onStartCommand")

        super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            mResultCode = intent.getIntExtra("code", -1)
            mResultData = intent.parcelable("data")
        }

        Log.d("screenMirrorService", "onStartCommand: mResultCode:$mResultCode mResultData:$mResultData")

        mImageReaderHandlerThread = HandlerThread("ImageReader")
        mImageReaderHandlerThread?.start()
        handler = Handler(mImageReaderHandlerThread!!.looper)
        orientationEventListener.enable()
        doMirror()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
        mImageReaderHandlerThread?.quitSafely()
        mBitmap = null
        orientationEventListener.disable()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun bitmapToByteArray(
        bitmap: Bitmap,
        width: Int,
        height: Int,
    ): ByteArray {
        val maxWidth = qualityData.resolution
        var newWidth = width
        var newHeight = height
        val longSide = maxOf(width, height)
        val shortSide = minOf(width, height)
        val scale = shortSide.toFloat() / longSide.toFloat()

        if (shortSide < maxWidth || longSide < maxWidth) {
        } else {
            if (width < height) {
                newWidth = maxWidth
                newHeight = (maxWidth / scale).toInt()
            } else {
                newWidth = (maxWidth / scale).toInt()
                newHeight = maxWidth
            }
        }

        val newBitmap = if (newWidth >= width && newHeight >= height) bitmap else bitmap.scale(newWidth, newHeight, true)

        val outputStream = ByteArrayOutputStream()
        val quality = qualityData.quality
        newBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val size = outputStream.size()
//        while (size > qualityData.maxSize && quality > 20) {
//            outputStream.reset()
//            quality -= 10
//            newBitmap.compress(quality, outputStream)
//            size = outputStream.size()
//        }
        Log.d("ScreenMirrorService","quality: $quality, size: $size, $newWidth x $newHeight")

        return outputStream.toByteArray()
    }

    private fun resize() {
        val width =
            if (isPortrait) {
                widthPortrait
            } else {
                widthLandscape
            }
        val height =
            if (isPortrait) {
                heightPortrait
            } else {
                heightLandscape
            }

        mVirtualDisplay?.surface = if (isPortrait) mImageReaderPortrait!!.surface else mImageReaderLandscape!!.surface
        mVirtualDisplay?.resize(width, height, mScreenDensity)
    }

    private fun doMirror() {
        mMediaProjection = mediaProjectionManager.getMediaProjection(mResultCode, mResultData!!)
        val width =
            if (isPortrait) {
                widthPortrait
            } else {
                widthLandscape
            }
        val height =
            if (isPortrait) {
                heightPortrait
            } else {
                heightLandscape
            }
        mImageReaderPortrait = ImageReader.newInstance(widthPortrait, heightPortrait, PixelFormat.RGBA_8888, 2)
        mImageReaderLandscape = ImageReader.newInstance(widthLandscape, heightLandscape, PixelFormat.RGBA_8888, 2)
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
        mVirtualDisplay =
            mMediaProjection?.createVirtualDisplay(
                "ScreenMirroringService", width, height, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                if (isPortrait) mImageReaderPortrait!!.surface else mImageReaderLandscape!!.surface,
                object : VirtualDisplay.Callback() {
                },
                null,
            )

        GlobalScope.launch {
            val result = currentSession()
            val userId = if (result is CurrentSessionResult.Success) {
                result.accountInfo.userId
            } else {
                null
            }

            Log.d("TAG", "doMirror: usedrId:$userId")
//            coreLogic.getSessionScope(userId!!).calls.setVideoPreview(ConversationId("", ""), PlatformView(mVirtualDisplay?.surface!!))
        }
    }

    private fun release() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay?.release()
            mVirtualDisplay = null
        }
        mImageReaderPortrait?.setOnImageAvailableListener(null, null)
        mImageReaderLandscape?.setOnImageAvailableListener(null, null)
        mImageReaderPortrait = null
        mImageReaderLandscape = null
        if (mMediaProjection != null) {
            mMediaProjection?.stop()
            mMediaProjection = null
        }
    }

    fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun getLatestImage(): ByteArray? {
        if (mBitmap == null) {
            return null
        }

        val outputStream = ByteArrayOutputStream()
        mBitmap!!.compress(Bitmap.CompressFormat.JPEG, qualityData.quality, outputStream)
        return outputStream.toByteArray()
    }

    companion object {
        var instance: ScreenMirrorService? = null
        var qualityData: DScreenMirrorQuality = DScreenMirrorQuality()
    }
}


fun getStartIntent(context: Context?, resultCode: Int, data: Intent?): Intent {
    val intent = Intent(context, ScreenMirrorService::class.java)
    intent.putExtra("code", resultCode)
    intent.putExtra("data", data)
    return intent
}

@Serializable
data class DScreenMirrorQuality(
    val quality: Int = 50,
    val resolution: Int = 720 // 480p, 720p，1080p，4k
)

fun <T> Context.getSystemServiceCompat(serviceClass: Class<T>): T = ContextCompat.getSystemService(this, serviceClass)!!

val mediaProjectionManager: MediaProjectionManager by lazy {
    WireApplication.instance.getSystemServiceCompat(MediaProjectionManager::class.java)
}