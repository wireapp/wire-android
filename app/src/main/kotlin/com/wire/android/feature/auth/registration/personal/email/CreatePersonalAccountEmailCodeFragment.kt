package com.wire.android.feature.auth.registration.personal.email

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.poovam.pinedittextfield.PinField.OnTextCompleteListener
import com.wire.android.R
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.extension.replaceFragment
import com.wire.android.core.extension.showKeyboardWithFocusOn
import com.wire.android.core.extension.withArgs
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.arg
import kotlinx.android.synthetic.main.fragment_create_personal_account_email_code.*
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountEmailCodeFragment : Fragment(R.layout.fragment_create_personal_account_email_code) {

    private val email by arg<String>(KEY_EMAIL)

    private val emailCodeViewModel: CreatePersonalAccountEmailCodeViewModel by viewModel()

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeActivateEmailData()
        observeNetworkConnectionError()

        initChangeMailListener()
        initDescriptionTextView()
        requestKeyboardFocus()
    }

    override fun onStart() {
        super.onStart()
        initPinCodeListener()
    }

    private fun initChangeMailListener() = createPersonalAccountEmailCodeChangeMailTextView.setOnClickListener {
        requireActivity().onBackPressed()
    }

    private fun initDescriptionTextView() {
        createPersonalAccountEmailCodeDescriptionTextView.text =
            getString(R.string.create_personal_account_email_code_description, email)
    }

    private fun initPinCodeListener() {
        createPersonalAccountEmailCodePinEditText.onTextCompleteListener = object : OnTextCompleteListener {
            override fun onTextComplete(enteredText: String): Boolean {
                emailCodeViewModel.activateEmail(email, enteredText)
                return false
            }
        }
    }

    private fun requestKeyboardFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(createPersonalAccountEmailCodePinEditText)
    }

    private fun observeActivateEmailData() {
        emailCodeViewModel.activateEmailLiveData.observe(viewLifecycleOwner) {
            it.onSuccess {
                showEnterNameScreen(it)
            }.onFailure {
                showGenericErrorDialog(getString(it.message))
                clearPinCode()
            }
        }
    }

    private fun showEnterNameScreen(code: String) =
        replaceFragment(R.id.createAccountLayoutContainer, CreatePersonalAccountEmailNameFragment.newInstance(email, code))

    private fun showGenericErrorDialog(message: String) {
        //TODO: proper dialog mechanism
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun clearPinCode() = createPersonalAccountEmailCodePinEditText.text?.clear()

    private fun observeNetworkConnectionError() {
        emailCodeViewModel.networkConnectionErrorLiveData.observe(viewLifecycleOwner) {
            showNetworkConnectionErrorDialog()
        }
    }

    //TODO: proper error
    private fun showNetworkConnectionErrorDialog() = showGenericErrorDialog("Network connection error!!!!")

    companion object {
        private const val KEY_EMAIL = "email"

        fun newInstance(email: String) = CreatePersonalAccountEmailCodeFragment().withArgs(
            KEY_EMAIL to email
        )
    }
}
