package com.wire.android.feature.auth.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.wire.android.R
import com.wire.android.core.extension.headingForAccessibility
import kotlinx.android.synthetic.main.fragment_login.*
import org.koin.android.viewmodel.ext.android.viewModel

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val loginViewModel: LoginViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAccessibility()
        initViewPager()
        initForgotPasswordButton()
    }

    private fun initAccessibility() {
        loginTitleTextView.headingForAccessibility()
    }

    private fun initViewPager() {
        val titles = listOf(
            getString(R.string.authentication_tab_layout_title_email),
            getString(R.string.authentication_tab_layout_title_phone)
        )
        loginViewPager.adapter = LoginViewPagerAdapter(this)
        TabLayoutMediator(loginTabLayout, loginViewPager) { tab, position ->
            val title = titles[position]
            tab.text = title
            tab.contentDescription = getString(R.string.login_tab_content_description, title, position + 1, titles.size)
        }.attach()
    }

    private fun initForgotPasswordButton() =
        loginForgotPasswordButton.setOnClickListener {
            openUri(loginViewModel.forgotPasswordUri)
        }

    //TODO pass to navigator maybe?
    private fun openUri(uri: Uri) = startActivity(Intent(Intent.ACTION_VIEW, uri))
}
