package com.wire.android.feature.auth.registration.personal.email

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.wire.android.R
import com.wire.android.core.extension.replaceFragment
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import kotlinx.android.synthetic.main.fragment_create_personal_account_email.*
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountEmailFragment :
    Fragment(R.layout.fragment_create_personal_account_email) {

    //TODO Add loading status
    private val emailViewModel: CreatePersonalAccountEmailViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeInputFocusData()
        observeEmailValidationData()
        observeActivationCodeData()
        observeNetworkConnectionError()
        initEmailChangedListener()
        initConfirmationButton()
    }

    private fun observeInputFocusData() {
        emailViewModel.textInputFocusedLiveData.observe(viewLifecycleOwner) {
            createPersonalAccountEmailEditText.requestFocus()
        }
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
        createPersonalAccountEmailConfirmationButton.setOnClickListener {
            emailViewModel.sendActivationCode(createPersonalAccountEmailEditText.text.toString())
        }
    }

    private fun observeActivationCodeData() {
        emailViewModel.sendActivationCodeLiveData.observe(viewLifecycleOwner) { response ->
            response.onSuccess {
                showEmailCodeScreen(it)
            }.onFailure {
                showGenericErrorDialog(it.message)
            }
        }
    }

    private fun showEmailCodeScreen(email: String) = replaceFragment(
        R.id.createAccountLayoutContainer, CreatePersonalAccountEmailCodeFragment.newInstance(email)
    )

    private fun observeNetworkConnectionError() {
        emailViewModel.networkConnectionErrorLiveData.observe(viewLifecycleOwner) {
            //TODO proper error dialog
            makeToast("Network error!!")
        }
    }

    //TODO: proper error dialogs
    private fun showGenericErrorDialog(messageResId: Int) = makeToast(getString(messageResId))

    //TODO: temporary method until we add error dialog structure.
    private fun makeToast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    companion object {
        fun newInstance() = CreatePersonalAccountEmailFragment()
    }
}
