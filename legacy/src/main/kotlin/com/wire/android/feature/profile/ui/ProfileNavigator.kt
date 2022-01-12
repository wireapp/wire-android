package com.wire.android.feature.profile.ui

import android.app.Activity

class ProfileNavigator {

    fun openProfileScreen(activity: Activity) {
        activity.startActivity(ProfileActivity.newIntent(activity))
    }
}
