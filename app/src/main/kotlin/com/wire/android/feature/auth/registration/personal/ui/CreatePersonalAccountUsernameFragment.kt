package com.wire.android.feature.auth.registration.personal.ui

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.wire.android.R
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.extension.headingForAccessibility
import com.wire.android.core.extension.showKeyboardWithFocusOn
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.ui.dialog.DialogBuilder
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.core.ui.navigation.Navigator
import com.wire.android.feature.auth.registration.ui.CreateAccountUsernameViewModel
import kotlinx.android.synthetic.main.fragment_create_personal_account_username.createPersonalAccountUsernameConfirmationButton
import kotlinx.android.synthetic.main.fragment_create_personal_account_username.createPersonalAccountUsernameEditText
import kotlinx.android.synthetic.main.fragment_create_personal_account_username.createPersonalAccountUsernameTextInputLayout
import kotlinx.android.synthetic.main.fragment_create_personal_account_username.createPersonalAccountUsernameTitleTextView
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountUsernameFragment : Fragment(R.layout.fragment_create_personal_account_username) {

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    private val usernameViewModel: CreateAccountUsernameViewModel by viewModel()

    private val navigator: Navigator by inject()

    private val dialogBuilder: DialogBuilder by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAccessibilityHeading()
        initConfirmationButton()
        observeButtonStatus()
        observeUsernameChanges()
        observeDialogErrors()
        initUsernameChangedListener()
        requestInitialFocus()
    }

    private fun setUpAccessibilityHeading() =
        createPersonalAccountUsernameTitleTextView.headingForAccessibility()

    private fun initConfirmationButton() = createPersonalAccountUsernameConfirmationButton.setOnClickListener {
        usernameViewModel.onConfirmationButtonClicked(createPersonalAccountUsernameEditText.text.toStringOrEmpty())
    }

    private fun observeButtonStatus() {
        usernameViewModel.confirmationButtonEnabled.observe(viewLifecycleOwner) {
            createPersonalAccountUsernameConfirmationButton.isEnabled = it
        }
    }

    private fun initUsernameChangedListener() {
        createPersonalAccountUsernameEditText.doAfterTextChanged {
            usernameViewModel.validateUsername(it.toStringOrEmpty())
        }
    }

    private fun observeUsernameChanges() {
        lifecycleScope.launchWhenCreated {
            usernameViewModel.generateUsername()
        }

        usernameViewModel.generatedUsernameLiveData.observe(viewLifecycleOwner) {
            createPersonalAccountUsernameEditText.setText(it)
        }

        usernameViewModel.usernameValidationLiveData.observe(viewLifecycleOwner) {
            it.fold(::showUsernameErrorMessage) { showMainScreen() }
        }
    }

    private fun showUsernameErrorMessage(errorMessage: ErrorMessage) {
        createPersonalAccountUsernameTextInputLayout.error = when (errorMessage) {
            ErrorMessage.EMPTY -> String.EMPTY
            else -> getString(errorMessage.message)
        }
    }

    private fun observeDialogErrors() {
        usernameViewModel.dialogErrorLiveData.observe(viewLifecycleOwner) {
            dialogBuilder.showErrorDialog(requireContext(), it)
        }
    }

    private fun requestInitialFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(createPersonalAccountUsernameEditText)
    }

    private fun showMainScreen() =
        navigator.main.openMainScreen(requireActivity())

    companion object {
        fun newInstance() = CreatePersonalAccountUsernameFragment()
    }
}
