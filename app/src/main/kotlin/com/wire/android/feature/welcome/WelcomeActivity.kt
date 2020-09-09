package com.wire.android.feature.welcome

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.R

class WelcomeActivity : AppCompatActivity(R.layout.activity_welcome) {

    companion object {
        fun newIntent(context: Context) = Intent(context, WelcomeActivity::class.java)
    }
}
