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
import com.wire.android.core.ui.navigation.Navigator
import com.wire.android.feature.auth.registration.ui.CreateAccountUsernameViewModel
import kotlinx.android.synthetic.main.fragment_create_personal_account_username.createPersonalAccountUsernameConfirmationButton
import kotlinx.android.synthetic.main.fragment_create_personal_account_username.createPersonalAccountUsernameEditText
import kotlinx.android.synthetic.main.fragment_create_personal_account_username.createPersonalAccountUsernameTitleTextView
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountUsernameFragment : Fragment(R.layout.fragment_create_personal_account_username) {

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    private val usernameViewModel: CreateAccountUsernameViewModel by viewModel()

    private val navigator: Navigator by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAccessibilityHeading()
        initConfirmationButton()
        observeButtonStatus()
        initUsernameChangedListener()
        requestInitialFocus()
    }

    private fun setUpAccessibilityHeading() =
        createPersonalAccountUsernameTitleTextView.headingForAccessibility()

    private fun initConfirmationButton() = createPersonalAccountUsernameConfirmationButton.setOnClickListener {
        showMainScreen(createPersonalAccountUsernameEditText.text.toStringOrEmpty())
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

    private fun requestInitialFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(createPersonalAccountUsernameEditText)
    }

    private fun showMainScreen(username: String) =
        navigator.main.openMainScreen(requireActivity())

    companion object {
        fun newInstance() =
            CreatePersonalAccountUsernameFragment()
    }
}
