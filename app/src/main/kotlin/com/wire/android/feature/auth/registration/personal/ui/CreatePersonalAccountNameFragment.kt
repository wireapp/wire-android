package com.wire.android.feature.auth.registration.personal.ui

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.wire.android.R
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.extension.headingForAccessibility
import com.wire.android.core.extension.replaceFragment
import com.wire.android.core.extension.showKeyboardWithFocusOn
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.extension.withArgs
import com.wire.android.core.ui.arg
import kotlinx.android.synthetic.main.fragment_create_personal_account_name.*
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountNameFragment : Fragment(R.layout.fragment_create_personal_account_name) {

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    private val nameViewModel: CreatePersonalAccountNameViewModel by viewModel()

    private val email by arg<String>(KEY_EMAIL)
    private val activationCode by arg<String>(KEY_ACTIVATION_CODE)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAccessibilityHeading()
        initConfirmationButton()
        observeButtonStatus()
        initNameChangedListener()
        requestInitialFocus()
    }

    private fun setUpAccessibilityHeading() =
        createPersonalAccountNameTitleTextView.headingForAccessibility()

    private fun initConfirmationButton() = createPersonalAccountNameConfirmationButton.setOnClickListener {
        showPasswordScreen(createPersonalAccountNameEditText.text.toStringOrEmpty())
    }

    private fun observeButtonStatus() {
        nameViewModel.continueEnabled.observe(viewLifecycleOwner) {
            createPersonalAccountNameConfirmationButton.isEnabled = it
        }
    }

    private fun initNameChangedListener() {
        createPersonalAccountNameEditText.doAfterTextChanged {
            nameViewModel.validateName(it.toStringOrEmpty())
        }
    }

    private fun requestInitialFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(createPersonalAccountNameEditText)
    }

    private fun showPasswordScreen(name: String) = replaceFragment(
        R.id.createAccountLayoutContainer,
        CreatePersonalAccountPasswordFragment.newInstance(name = name, email = email, activationCode = activationCode)
    )

    companion object {
        private const val KEY_EMAIL = "email"
        private const val KEY_ACTIVATION_CODE = "activationCode"

        fun newInstance(email: String, activationCode: String) =
            CreatePersonalAccountNameFragment().withArgs(
                KEY_EMAIL to email, KEY_ACTIVATION_CODE to activationCode
            )
    }
}
