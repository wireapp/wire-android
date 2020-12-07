package com.wire.android.feature.auth.registration.pro.email

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
import com.wire.android.feature.auth.registration.ui.CreateAccountEmailViewModel
import kotlinx.android.synthetic.main.fragment_create_pro_account_team_email.createProAccountTeamEmailEditText
import kotlinx.android.synthetic.main.fragment_create_pro_account_team_email.createProAccountTeamEmailInputConfirmationButton
import kotlinx.android.synthetic.main.fragment_create_pro_account_team_email.createProAccountTeamEmailTitleTextView
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class CreateProAccountTeamEmailFragment : Fragment(R.layout.fragment_create_pro_account_team_email) {

    //TODO Add loading status
    private val emailViewModel: CreateAccountEmailViewModel by viewModel()

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    private val dialogBuilder: DialogBuilder by inject()

    private val navigator: Navigator by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeInputFocusData()
        observeEmailValidationData()
        observeActivationCodeData()

        initCreateAccountTitle()
        initEmailChangedListener()
        initConfirmationButton()
    }

    private fun initCreateAccountTitle() {
        createProAccountTeamEmailTitleTextView.headingForAccessibility()
    }

    private fun observeInputFocusData() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(createProAccountTeamEmailEditText)
    }

    private fun observeEmailValidationData() {
        emailViewModel.confirmationButtonEnabled.observe(viewLifecycleOwner) {
            updateConfirmationButtonStatus(it)
        }
    }

    private fun updateConfirmationButtonStatus(enabled: Boolean) {
        createProAccountTeamEmailInputConfirmationButton.isEnabled = enabled
    }

    private fun initEmailChangedListener() {
        createProAccountTeamEmailEditText.doAfterTextChanged {
            emailViewModel.validateEmail(it.toStringOrEmpty())
        }
    }

    private fun initConfirmationButton() {
        updateConfirmationButtonStatus(false)
        createProAccountTeamEmailInputConfirmationButton.setOnClickListener {
            emailViewModel.sendActivationCode(createProAccountTeamEmailEditText.text.toStringOrEmpty())
        }
    }

    private fun observeActivationCodeData() {
        emailViewModel.sendActivationCodeLiveData.observe(viewLifecycleOwner) { response ->
            response.onSuccess {
                showEmailVerificationCodeScreen(it)
            }.onFailure {
                showErrorDialog(it)
            }
        }
    }

    private fun showEmailVerificationCodeScreen(email: String) =
        navigator.createAccount.openProAccountTeamEmailVerificationScreen(requireActivity(), email)

    private fun showErrorDialog(errorMessage: ErrorMessage) = dialogBuilder.showErrorDialog(requireContext(), errorMessage)

    companion object {
        fun newInstance() = CreateProAccountTeamEmailFragment()
    }
}
