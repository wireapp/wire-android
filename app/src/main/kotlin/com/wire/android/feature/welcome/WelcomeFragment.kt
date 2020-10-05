package com.wire.android.feature.welcome

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.core.extension.headingForAccessibility
import com.wire.android.core.ui.navigation.Navigator
import kotlinx.android.synthetic.main.fragment_welcome.*
import org.koin.android.ext.android.inject

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    private val navigator: Navigator by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initWelcomeTitle()
        initCreateAccountButton()
        initLoginButton()
    }

    private fun initWelcomeTitle() {
        welcomeTitleTextView.headingForAccessibility()
    }

    private fun initCreateAccountButton() =
        welcomeCreateAccountButton.setOnClickListener {
            navigator.createAccount.openCreateAccount(requireContext())
        }

    private fun initLoginButton() =
        welcomeLoginButton.setOnClickListener {
            navigator.login.openLogin(requireContext())
        }
}
