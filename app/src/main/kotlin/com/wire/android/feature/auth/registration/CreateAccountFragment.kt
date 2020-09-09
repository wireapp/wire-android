package com.wire.android.feature.auth.registration

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.core.extension.replaceFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountEmailFragment
import com.wire.android.feature.auth.registration.pro.team.CreateProAccountTeamNameFragment
import kotlinx.android.synthetic.main.fragment_create_account.*

class CreateAccountFragment : Fragment(R.layout.fragment_create_account) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCreatePersonalAccount()
        initCreateProAccount()
    }

    private fun initCreatePersonalAccount() =
        createPersonalAccountLayoutContainer.setOnClickListener {
            replaceFragment(
                R.id.createAccountLayoutContainer,
                CreatePersonalAccountEmailFragment.newInstance()
            )
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
