package com.wire.android.feature.auth.registration.personal.email

import android.os.Bundle
import android.view.View
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
import com.wire.android.core.ui.dialog.DialogBuilder
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.core.ui.dialog.NetworkErrorMessage
import kotlinx.android.synthetic.main.fragment_create_personal_account_email_code.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountEmailCodeFragment : Fragment(R.layout.fragment_create_personal_account_email_code) {

    private val email by arg<String>(KEY_EMAIL)

    private val emailCodeViewModel: CreatePersonalAccountEmailCodeViewModel by viewModel()

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    private val dialogBuilder: DialogBuilder by inject()

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
                showErrorDialog(it)
                clearPinCode()
            }
        }
    }

    private fun showEnterNameScreen(code: String) =
        replaceFragment(R.id.createAccountLayoutContainer, CreatePersonalAccountEmailNameFragment.newInstance(email, code))

    private fun showErrorDialog(errorMessage: ErrorMessage) = dialogBuilder.showErrorDialog(requireContext(), errorMessage)

    private fun clearPinCode() = createPersonalAccountEmailCodePinEditText.text?.clear()

    private fun observeNetworkConnectionError() {
        emailCodeViewModel.networkConnectionErrorLiveData.observe(viewLifecycleOwner) {
            showErrorDialog(NetworkErrorMessage)
        }
    }

    companion object {
        private const val KEY_EMAIL = "email"

        fun newInstance(email: String) = CreatePersonalAccountEmailCodeFragment().withArgs(
            KEY_EMAIL to email
        )
    }
}
