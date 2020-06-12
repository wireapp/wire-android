package com.wire.android.feature.auth.registration

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wire.android.R
import kotlinx.android.synthetic.main.activity_create_account.*

class CreateAccountActivity : AppCompatActivity(R.layout.activity_create_account) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        replaceFragment(R.id.activityCreateAccountLayoutContainer, CreateAccountFragment.newInstance(), false)
        initBackButton()
    }

    private fun initBackButton() {
        createAccountBackButton.setOnClickListener { onBackPressed() }
    }
}