package com.wire.android.util.extension

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

fun Context.checkPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
}

fun Context.isGoogleServicesAvailable(): Boolean {
    val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
    return status == ConnectionResult.SUCCESS
}

fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
