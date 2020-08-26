package com.wire.android.feature.auth.registration.personal.email

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.wire.android.R
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.extension.headingForAccessibility
import com.wire.android.core.extension.showKeyboardWithFocusOn
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.extension.toast
import com.wire.android.core.extension.withArgs
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.arg
import com.wire.android.core.ui.dialog.DialogBuilder
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.core.ui.dialog.NetworkErrorMessage
import kotlinx.android.synthetic.main.fragment_create_personal_account_email_password.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountEmailPasswordFragment : Fragment(R.layout.fragment_create_personal_account_email_password) {

    private val passwordViewModel: CreatePersonalAccountEmailPasswordViewModel by viewModel()

    private val inputFocusViewModel : InputFocusViewModel by viewModel()

    private val dialogBuilder: DialogBuilder by inject()

    private val name by arg<String>(KEY_NAME)
    private val email by arg<String>(KEY_EMAIL)
    private val activationCode by arg<String>(KEY_ACTIVATION_CODE)
    private val password: String get() = createPersonalAccountEmailPasswordEditText.text.toStringOrEmpty()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAccessibilityHeading()

        observePasswordValidationData()
        observeRegistrationData()
        observeNetworkConnectionError()

        initPasswordPolicyText()
        initPasswordChangedListener()
        initConfirmationButton()
        requestInitialFocus()
    }

    private fun setUpAccessibilityHeading() =
        createPersonalAccountWithEmailPasswordTitleTextView.headingForAccessibility()

    private fun observePasswordValidationData() {
        passwordViewModel.continueEnabledLiveData.observe(viewLifecycleOwner) {
            createPersonalAccountEmailPasswordConfirmationButton.isEnabled = it
        }
    }

    private fun observeRegistrationData() {
        passwordViewModel.registerStatusLiveData.observe(viewLifecycleOwner) {
            it.onSuccess {
                showMainScreen()
            }.onFailure(::showErrorDialog)
        }
    }

    private fun observeNetworkConnectionError() {
        passwordViewModel.networkConnectionErrorLiveData.observe(viewLifecycleOwner) {
            showErrorDialog(NetworkErrorMessage)
        }
    }

    private fun initPasswordPolicyText() {
        createPersonalAccountEmailPasswordPolicyTextView.text =
            getString(R.string.create_personal_account_password_policy_info, passwordViewModel.minPasswordLength())
    }

    private fun initPasswordChangedListener() {
        createPersonalAccountEmailPasswordEditText.doAfterTextChanged {
            passwordViewModel.validatePassword(it.toStringOrEmpty())
        }
    }

    private fun initConfirmationButton() =
        createPersonalAccountEmailPasswordConfirmationButton.setOnClickListener { registerNewUser() }

    private fun requestInitialFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(createPersonalAccountEmailPasswordEditText)
    }

    private fun registerNewUser() =
        passwordViewModel.registerUser(name = name, email = email, code = activationCode, password = password)

    private fun showMainScreen() {
        //TODO implement main screen and navigate
        toast("User registered! name: $name, email: $email")
    }

    private fun showErrorDialog(message: ErrorMessage) = dialogBuilder.showErrorDialog(requireContext(), message)

    companion object {
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_ACTIVATION_CODE = "activationCode"

        fun newInstance(name: String, email: String, activationCode: String) =
            CreatePersonalAccountEmailPasswordFragment().withArgs(
                KEY_NAME to name,
                KEY_EMAIL to email,
                KEY_ACTIVATION_CODE to activationCode
            )
    }
}
