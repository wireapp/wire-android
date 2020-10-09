package com.wire.android.feature.auth.registration

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.core.extension.replaceFragment
import com.wire.android.core.ui.navigation.Navigator
import com.wire.android.feature.auth.registration.pro.team.CreateProAccountTeamNameFragment
import kotlinx.android.synthetic.main.fragment_create_account.*
import org.koin.android.ext.android.inject

class CreateAccountFragment : Fragment(R.layout.fragment_create_account) {

    private val navigator: Navigator by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCreatePersonalAccount()
        initCreateProAccount()
    }

    private fun initCreatePersonalAccount() =
        createPersonalAccountLayoutContainer.setOnClickListener {
            navigator.createAccount.openEmailScreen(requireActivity())
        }

    private fun initCreateProAccount() =
        createProAccountLayoutContainer.setOnClickListener {
            replaceFragment(
                R.id.createAccountLayoutContainer,
                CreateProAccountTeamNameFragment.newInstance()
            )
        }

    companion object {
        fun newInstance() = CreateAccountFragment()
    }
}
