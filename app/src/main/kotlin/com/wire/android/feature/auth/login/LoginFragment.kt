package com.wire.android.feature.auth.login

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.wire.android.core.extension.headingForAccessibility
import com.wire.android.R
import kotlinx.android.synthetic.main.fragment_login.*

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAccessibility()
    }

    private fun initAccessibility() {
        loginTitleTextView.headingForAccessibility()
    }
}
