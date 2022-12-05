package com.wire.android.appUpdate

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.wire.android.appLogger
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WireAppUpdateManager @Inject constructor(context: Context) {
    private val appUpdateManager = AppUpdateManagerFactory.create(context)

    @Suppress("TooGenericExceptionCaught")
    suspend fun isAppUpdateAvailable(): Boolean = try {
        appLogger.i("$TAG Getting AppUpdateInfo")
        val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
        appLogger.i("$TAG Got AppUpdateInfo $appUpdateInfo")
        appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
    } catch (e: Throwable) {
        appLogger.e("$TAG Failure while getting AppUpdateInfo", e)
        false
    }

    fun updateTheApp(activity: ComponentActivity) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                appLogger.e("$TAG Got AppUpdateInfo, updating the app...")
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    activity,
                    REQUEST_CODE
                )
            }
            .addOnFailureListener { appLogger.e("$TAG Failure while getting AppUpdateInfo for updating", it) }
    }

    companion object {
        const val TAG = "WireAppUpdateManager"
        val REQUEST_CODE = "update_app_request_code".hashCode()
    }
}
