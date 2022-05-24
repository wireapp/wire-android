package com.wire.android.util.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.ScreenMode
import com.wire.android.navigation.getCurrentNavigationItem

fun Activity.updateScreenSettings(navController: NavController) {
    val screenMode = navController.getCurrentNavigationItem()?.screenMode
    println("cyka screen settings: $screenMode")
    updateScreenSettings(screenMode)
}

fun Activity.setScreenSettingsOnStart(startDestination: String) {
    val screenMode = NavigationItem.fromRoute(startDestination)?.screenMode
    println("cyka start screen settings: $screenMode")
    updateScreenSettings(screenMode)
}

private fun Activity.updateScreenSettings(screenMode: ScreenMode?) {
    when (screenMode) {
        ScreenMode.WAKE_UP -> wakeUpDevice()
        ScreenMode.KEEP_ON -> addScreenOnFlags()
        else -> removeScreenOnFlags()
    }
}

private fun Activity.wakeUpDevice() {

//    val pm = this.getSystemService(Context.POWER_SERVICE) as PowerManager
//    val isScreenOn = pm.isInteractive
//    val flags = (PowerManager.FULL_WAKE_LOCK
//            or PowerManager.ACQUIRE_CAUSES_WAKEUP
//            or PowerManager.ON_AFTER_RELEASE)
//    if (!isScreenOn) {
//        val wakeLock = pm.newWakeLock(flags, "my_app:full_lock")
//        wakeLock.acquire(20000)
//    }

    window.addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    } else {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        with(getSystemService(AppCompatActivity.KEYGUARD_SERVICE) as KeyguardManager) {
            requestDismissKeyguard(this@wakeUpDevice, null)
        }
    } else {
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
    }

//    addScreenOnFlags()

//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
//        setShowWhenLocked(true)
//        setTurnScreenOn(true)
//    } else {
//        window.addFlags(
//            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
////                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//        )
//    }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//          with(getSystemService(AppCompatActivity.KEYGUARD_SERVICE) as KeyguardManager) {
//            requestDismissKeyguard(this@wakeUpDevice, null)
//        } else {
//              window.addFlags(
//                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//        )
//        }
//    }
}

private fun Activity.addScreenOnFlags() {
    window.addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
    )
}

private fun Activity.removeScreenOnFlags() {
    window.clearFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(false)
        setTurnScreenOn(false)
    } else {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }
}
