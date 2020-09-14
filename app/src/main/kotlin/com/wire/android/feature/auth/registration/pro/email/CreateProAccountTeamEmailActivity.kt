package com.wire.android.feature.auth.registration.pro.email

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.R

class CreateProAccountTeamEmailActivity : AppCompatActivity(
    R.layout.activity_create_pro_account_team_email
) {
    companion object {
        fun newIntent(context: Context) = Intent(
            context,
            CreateProAccountTeamEmailActivity::class.java
        )
    }
}