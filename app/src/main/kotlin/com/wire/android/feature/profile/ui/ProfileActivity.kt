package com.wire.android.feature.profile.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.R

class ProfileActivity : AppCompatActivity(R.layout.activity_profile) {

    companion object {
        fun newIntent(context: Context) = Intent(context, ProfileActivity::class.java)
    }
}
