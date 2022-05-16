package com.wire.android.util.ui

import android.app.Activity
import android.app.KeyguardManager
import android.os.Build
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.wire.android.navigation.ScreenMode
import com.wire.android.navigation.getCurrentNavigationItem

fun Activity.updateScreenSettings(navController: NavController) {
    when (navController.getCurrentNavigationItem()?.screenMode) {
        ScreenMode.WAKE_UP -> wakeUpDevice()
        ScreenMode.KEEP_ON -> addScreenOnFlags()
        else -> removeScreenOnFlags()
    }
}

private fun Activity.wakeUpDevice() {
    addScreenOnFlags()

//    with(getSystemService(AppCompatActivity.KEYGUARD_SERVICE) as KeyguardManager) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            requestDismissKeyguard(this@wakeUpDevice, null)
//        }
//    }
}

private fun Activity.addScreenOnFlags() {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
//        setShowWhenLocked(true)
//        setTurnScreenOn(true)
//    } else {
    window.addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
    )
//    }
}

private fun Activity.removeScreenOnFlags() {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
//        setShowWhenLocked(false)
//        setTurnScreenOn(false)
//    } else {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
//    }
}
