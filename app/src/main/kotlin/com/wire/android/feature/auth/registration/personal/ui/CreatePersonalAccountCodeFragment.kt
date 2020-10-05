package com.wire.android.feature.auth.registration.personal.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.navArgs
import com.poovam.pinedittextfield.PinField.OnTextCompleteListener
import com.wire.android.R
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.extension.replaceFragment
import com.wire.android.core.extension.showKeyboardWithFocusOn
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.ui.dialog.DialogBuilder
import com.wire.android.core.ui.dialog.ErrorMessage
import kotlinx.android.synthetic.main.fragment_create_personal_account_code.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountCodeFragment : Fragment(R.layout.fragment_create_personal_account_code) {

    private val args: CreatePersonalAccountCodeFragmentArgs by navArgs()
    private val email: String get() = args.email

    private val codeViewModel: CreatePersonalAccountCodeViewModel by viewModel()

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    private val dialogBuilder: DialogBuilder by inject()

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
                codeViewModel.activateEmail(email, enteredText)
                return false
            }
        }
    }

    private fun requestKeyboardFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(createPersonalAccountCodePinEditText)
    }

    private fun observeActivateEmailData() {
        codeViewModel.activateEmailLiveData.observe(viewLifecycleOwner) {
            it.onSuccess {
                showEnterNameScreen(it)
            }.onFailure {
                showErrorDialog(it)
                clearPinCode()
            }
        }
    }

    private fun showEnterNameScreen(code: String) =
        replaceFragment(R.id.createAccountLayoutContainer, CreatePersonalAccountNameFragment.newInstance(email, code))

    private fun showErrorDialog(errorMessage: ErrorMessage) = dialogBuilder.showErrorDialog(requireContext(), errorMessage)

    private fun clearPinCode() = createPersonalAccountCodePinEditText.text?.clear()
}
