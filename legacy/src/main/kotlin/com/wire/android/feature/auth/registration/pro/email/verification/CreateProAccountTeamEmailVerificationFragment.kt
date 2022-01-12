package com.wire.android.feature.auth.registration.pro.email.verification

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.poovam.pinedittextfield.PinField
import com.wire.android.R
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.extension.showKeyboardWithFocusOn
import com.wire.android.core.extension.withArgs
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.arg
import com.wire.android.core.ui.dialog.DialogBuilder
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.feature.auth.registration.ui.CreateAccountEmailVerificationCodeViewModel
import kotlinx.android.synthetic.main.fragment_create_pro_account_team_email_verification.createProAccountEmailVerificationCodeChangeEmailTextView
import kotlinx.android.synthetic.main.fragment_create_pro_account_team_email_verification.createProAccountEmailVerificationCodeDescriptionTextView
import kotlinx.android.synthetic.main.fragment_create_pro_account_team_email_verification.createProAccountEmailVerificationCodePinEditText
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class CreateProAccountTeamEmailVerificationFragment : Fragment(R.layout.fragment_create_pro_account_team_email_verification) {

    private val emailVerificationCodeViewModel: CreateAccountEmailVerificationCodeViewModel by viewModel()

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    private val dialogBuilder: DialogBuilder by inject()

    private val selectedEmail by arg<String>(EMAIL_BUNDLE_KEY)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeActivatedEmailData()

        initChangeMailListener()
        initDescriptionTextView()
        requestKeyboardFocus()
    }

    override fun onStart() {
        super.onStart()
        initPinCodeListener()
    }

    private fun initChangeMailListener() = createProAccountEmailVerificationCodeChangeEmailTextView.setOnClickListener {
        requireActivity().onBackPressed()
    }

    private fun initDescriptionTextView() {
        createProAccountEmailVerificationCodeDescriptionTextView.text =
            getString(R.string.create_personal_account_code_description, selectedEmail)
    }

    private fun initPinCodeListener() {
        createProAccountEmailVerificationCodePinEditText.onTextCompleteListener = object : PinField.OnTextCompleteListener {
            override fun onTextComplete(enteredText: String): Boolean {
                emailVerificationCodeViewModel.activateEmail(selectedEmail, enteredText)
                return false
            }
        }
    }

    private fun requestKeyboardFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(createProAccountEmailVerificationCodePinEditText)
    }

    private fun observeActivatedEmailData() {
        emailVerificationCodeViewModel.activateEmailLiveData.observe(viewLifecycleOwner) {
            it.onSuccess {
                //Go to next screen
            }.onFailure { errorMessage ->
                showErrorDialog(errorMessage)
                clearPinCode()
            }
        }
    }

    private fun showErrorDialog(errorMessage: ErrorMessage) = dialogBuilder.showErrorDialog(requireContext(), errorMessage)

    private fun clearPinCode() = createProAccountEmailVerificationCodePinEditText.text?.clear()

    companion object {
        private const val EMAIL_BUNDLE_KEY = "email"

        fun newInstance(email: String) =
            CreateProAccountTeamEmailVerificationFragment().withArgs(EMAIL_BUNDLE_KEY to email)
    }
}
