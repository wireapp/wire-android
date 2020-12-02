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
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.dialog.DialogBuilder
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.core.ui.navigation.Navigator
import kotlinx.android.synthetic.main.fragment_create_personal_account_email.createPersonalAccountEmailConfirmationButton
import kotlinx.android.synthetic.main.fragment_create_personal_account_email.createPersonalAccountEmailEditText
import kotlinx.android.synthetic.main.fragment_create_personal_account_email.createPersonalAccountEmailTitleTextView
import kotlinx.android.synthetic.main.fragment_create_pro_account_team_email.createProAccountTeamEmailTextInputLayout
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountEmailFragment : Fragment(R.layout.fragment_create_personal_account_email) {

    private val navigator: Navigator by inject()

    //TODO Add loading status
    private val emailViewModel: CreatePersonalAccountEmailViewModel by viewModel()

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    private val dialogBuilder: DialogBuilder by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCreateAccountTitle()
        observeInputFocusData()
        observeEmailValidationData()
        observeActivationCodeData()
        initEmailChangedListener()
        initConfirmationButton()
    }

    private fun initCreateAccountTitle() {
        createPersonalAccountEmailTitleTextView.headingForAccessibility()
    }

    private fun observeInputFocusData() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(createPersonalAccountEmailEditText)
    }

    private fun observeEmailValidationData() {
        emailViewModel.isValidEmailLiveData.observe(viewLifecycleOwner) {
            updateConfirmationButtonStatus(it)
        }
        emailViewModel.emailValidationErrorLiveData.observe(viewLifecycleOwner) {
            createProAccountTeamEmailTextInputLayout.error = getString(it.message)
        }
    }

    private fun updateConfirmationButtonStatus(enabled: Boolean) {
        createPersonalAccountEmailConfirmationButton.isEnabled = enabled
    }

    private fun initEmailChangedListener() {
        createPersonalAccountEmailEditText.doAfterTextChanged {
            emailViewModel.validateEmail(it.toStringOrEmpty())
        }
    }

    private fun initConfirmationButton() {
        updateConfirmationButtonStatus(false)
        createPersonalAccountEmailConfirmationButton.setOnClickListener {
            emailViewModel.sendActivationCode(createPersonalAccountEmailEditText.text.toStringOrEmpty())
        }
    }

    private fun observeActivationCodeData() {
        emailViewModel.sendActivationCodeLiveData.observe(viewLifecycleOwner) { response ->
            response.onSuccess {
                showEmailCodeScreen(it)
            }.onFailure {
                showErrorDialog(it)
            }
        }
    }

    private fun showEmailCodeScreen(email: String) = navigator.createAccount.openPersonalAccountCodeScreen(requireActivity(), email)

    private fun showErrorDialog(errorMessage: ErrorMessage) = dialogBuilder.showErrorDialog(requireContext(), errorMessage)

    companion object {
        fun newInstance() = CreatePersonalAccountEmailFragment()
    }
}
