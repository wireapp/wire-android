package com.wire.android.feature.auth.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.R
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(R.layout.activity_login) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBackButton()
    }

    private fun initBackButton() = loginBackButton.setOnClickListener { onBackPressed() }

    companion object {
        fun newIntent(context: Context) = Intent(context, LoginActivity::class.java)
    }
}
