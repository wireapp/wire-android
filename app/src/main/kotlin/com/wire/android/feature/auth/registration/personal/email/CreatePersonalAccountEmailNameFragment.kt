package com.wire.android.feature.auth.registration.personal.email

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.wire.android.R
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.extension.showKeyboardWithFocusOn
import kotlinx.android.synthetic.main.create_personal_account_email_name.*
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountEmailNameFragment : Fragment(R.layout.create_personal_account_email_name) {

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    private val nameViewModel: CreatePersonalAccountEmailNameViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initConfirmationButton()
        observeButtonStatus()
        initNameChangedListener()
        requestInitialFocus()
    }

    private fun initConfirmationButton() = createPersonalAccountEmailNameConfirmationButton.setOnClickListener {
        showPasswordScreen()
    }

    private fun observeButtonStatus() {
        nameViewModel.continueEnabled.observe(viewLifecycleOwner) {
            createPersonalAccountEmailNameConfirmationButton.isEnabled = it
        }
    }

    private fun initNameChangedListener() {
        createPersonalAccountEmailNameEditText.doAfterTextChanged {
            nameViewModel.validateName(it.toString())
        }
    }

    private fun requestInitialFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(createPersonalAccountEmailNameEditText)
    }

    private fun showPasswordScreen() {
        //TODO: open password screen
    }

    companion object {
        fun newInstance() = CreatePersonalAccountEmailNameFragment()
    }
}
