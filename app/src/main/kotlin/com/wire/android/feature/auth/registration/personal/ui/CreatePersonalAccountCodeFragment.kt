package com.wire.android.feature.auth.registration.personal.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.poovam.pinedittextfield.PinField.OnTextCompleteListener
import com.wire.android.R
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.extension.showKeyboardWithFocusOn
import com.wire.android.core.extension.withArgs
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.arg
import com.wire.android.core.ui.dialog.DialogBuilder
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.core.ui.navigation.Navigator
import com.wire.android.feature.auth.registration.ui.CreateAccountEmailVerificationCodeViewModel
import kotlinx.android.synthetic.main.fragment_create_personal_account_code.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountCodeFragment : Fragment(R.layout.fragment_create_personal_account_code) {

    private val email by arg<String>(KEY_EMAIL)

    private val emailVerificationCodeViewModel: CreateAccountEmailVerificationCodeViewModel by viewModel()

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    private val dialogBuilder: DialogBuilder by inject()

    private val navigator: Navigator by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeActivateEmailData()

        initChangeMailListener()
        initDescriptionTextView()
        requestKeyboardFocus()
    }

    override fun onStart() {
        super.onStart()
        initPinCodeListener()
    }

    private fun initChangeMailListener() = createPersonalAccountCodeChangeMailTextView.setOnClickListener {
        requireActivity().onBackPressed()
    }

    private fun initDescriptionTextView() {
        createPersonalAccountCodeDescriptionTextView.text =
            getString(R.string.create_personal_account_code_description, email)
    }

    private fun initPinCodeListener() {
        createPersonalAccountCodePinEditText.onTextCompleteListener = object : OnTextCompleteListener {
            override fun onTextComplete(enteredText: String): Boolean {
                emailVerificationCodeViewModel.activateEmail(email, enteredText)
                return false
            }
        }
    }

    private fun requestKeyboardFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(createPersonalAccountCodePinEditText)
    }

    private fun observeActivateEmailData() {
        emailVerificationCodeViewModel.activateEmailLiveData.observe(viewLifecycleOwner) {
            it.onSuccess {
                showEnterNameScreen(it)
            }.onFailure {
                showErrorDialog(it)
                clearPinCode()
            }
        }
    }

    private fun showEnterNameScreen(code: String) = navigator.createAccount.openPersonalAccountNameScreen(requireActivity(), email, code)

    private fun showErrorDialog(errorMessage: ErrorMessage) = dialogBuilder.showErrorDialog(requireContext(), errorMessage)

    private fun clearPinCode() = createPersonalAccountCodePinEditText.text?.clear()

    companion object {
        private const val KEY_EMAIL = "email"

        fun newInstance(email: String) =
            CreatePersonalAccountCodeFragment().withArgs(KEY_EMAIL to email)
    }
}
