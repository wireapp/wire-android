package com.wire.android.feature.welcome

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.feature.auth.registration.CreateAccountActivity
import kotlinx.android.synthetic.main.fragment_welcome.*

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCreateAccountButton()
    }

    private fun initCreateAccountButton() {
        welcomeCreateAccountButton.setOnClickListener {
            startActivity(Intent(requireActivity(), CreateAccountActivity::class.java))
        }
    }

    companion object {
        fun newInstance() = WelcomeFragment()
    }
}
