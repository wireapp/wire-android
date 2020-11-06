package com.wire.android.feature.conversation.list

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.R

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    companion object {
        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
