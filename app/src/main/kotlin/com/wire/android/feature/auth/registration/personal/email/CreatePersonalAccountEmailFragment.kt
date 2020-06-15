package com.wire.android.feature.auth.registration.personal.email

import android.os.Bundle
import android.view.View
import androidx.lifecycle.observe
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.wire.android.R
import kotlinx.android.synthetic.main.fragment_create_personal_account_email.*
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountEmailFragment : Fragment(
    R.layout.fragment_create_personal_account_email
) {

    //TODO Add loading status
    private val emailViewModel: CreatePersonalAccountEmailViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEmailValidationData()
        initEmailChangedListener()
        initConfirmationButton()
    }

    private fun observeEmailValidationData() {
        emailViewModel.isValidEmailLiveData.observe(viewLifecycleOwner) {
            updateConfirmationButtonStatus(it)
        }
    }

    private fun updateConfirmationButtonStatus(enabled: Boolean) {
        createPersonalAccountEmailConfirmationButton.isEnabled = enabled
    }

    private fun initEmailChangedListener() {
        createPersonalAccountEmailEditText.doAfterTextChanged {
            emailViewModel.validateEmail(it.toString())
        }
    }

    private fun initConfirmationButton() {
        updateConfirmationButtonStatus(false)
//        createPersonalAccountEmailConfirmationButton.setOnClickListener {
//            emailViewModel.sendActivationCode(
//                createPersonalAccountEmailEditText.text.toString()
//            )
//        }
    }

    companion object {
        fun newInstance() = CreatePersonalAccountEmailFragment()
    }
}