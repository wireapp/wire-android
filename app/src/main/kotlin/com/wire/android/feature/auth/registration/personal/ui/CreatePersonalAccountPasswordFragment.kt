package com.wire.android.feature.auth.registration.personal.ui

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.extension.headingForAccessibility
import com.wire.android.core.extension.showKeyboardWithFocusOn
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.extension.withArgs
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.arg
import com.wire.android.core.ui.dialog.DialogBuilder
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.core.ui.navigation.Navigator
import kotlinx.android.synthetic.main.fragment_create_personal_account_password.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountPasswordFragment : Fragment(R.layout.fragment_create_personal_account_password) {

    private val passwordViewModel: CreatePersonalAccountPasswordViewModel by viewModel()

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    private val dialogBuilder: DialogBuilder by inject()

    private val navigator: Navigator by inject()

    private val name by arg<String>(KEY_NAME)
    private val email by arg<String>(KEY_EMAIL)
    private val activationCode by arg<String>(KEY_ACTIVATION_CODE)
    private val password: String get() = createPersonalAccountPasswordEditText.text.toStringOrEmpty()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAccessibilityHeading()

        observePasswordValidationData()
        observeRegistrationData()

        initPasswordPolicyText()
        initPasswordChangedListener()
        initConfirmationButton()
        requestInitialFocus()
    }

    private fun setUpAccessibilityHeading() =
        createPersonalAccountPasswordTitleTextView.headingForAccessibility()

    private fun observePasswordValidationData() {
        passwordViewModel.continueEnabledLiveData.observe(viewLifecycleOwner) {
            createPersonalAccountPasswordConfirmationButton.isEnabled = it
        }
    }

    private fun observeRegistrationData() {
        passwordViewModel.registerStatusLiveData.observe(viewLifecycleOwner) {
            it.onSuccess {
                showMainScreen()
            }.onFailure(::showErrorDialog)
        }
    }

    private fun initPasswordPolicyText() {
        createPersonalAccountEmailPasswordPolicyTextView.text =
            getString(R.string.create_personal_account_password_policy_info, passwordViewModel.minPasswordLength())
    }

    private fun initPasswordChangedListener() {
        createPersonalAccountPasswordEditText.doAfterTextChanged {
            passwordViewModel.validatePassword(it.toStringOrEmpty())
        }
    }

    private fun initConfirmationButton() =
        createPersonalAccountPasswordConfirmationButton.setOnClickListener { registerNewUser() }

    private fun requestInitialFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(createPersonalAccountPasswordEditText)
    }

    private fun registerNewUser() =
        passwordViewModel.registerUser(name = name, email = email, code = activationCode, password = password)

    private fun showMainScreen() = navigator.main.openMainScreen(requireContext())

    private fun showErrorDialog(message: ErrorMessage) = dialogBuilder.showErrorDialog(requireContext(), message)

    companion object {
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_ACTIVATION_CODE = "activationCode"

        fun newInstance(name: String, email: String, activationCode: String) =
            CreatePersonalAccountPasswordFragment().withArgs(
                KEY_NAME to name,
                KEY_EMAIL to email,
                KEY_ACTIVATION_CODE to activationCode
            )
    }
}
