package com.wire.android.feature.auth.registration.personal.email

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.poovam.pinedittextfield.PinField.OnTextCompleteListener
import com.wire.android.R
import com.wire.android.core.extension.showKeyboard
import com.wire.android.core.extension.withArgs
import com.wire.android.core.ui.arg
import kotlinx.android.synthetic.main.fragment_create_personal_account_email_code.*

class CreatePersonalAccountEmailCodeFragment : Fragment(R.layout.fragment_create_personal_account_email_code) {

    private val email by arg<String>(KEY_EMAIL)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initChangeMailListener()
        initDescriptionTextView()
        initPinCodeListener()
        showKeyboard()
    }

    private fun initDescriptionTextView() {
        createPersonalAccountEmailCodeDescriptionTextView.text =
            getString(R.string.create_personal_account_email_code_description, email)
    }

    private fun initPinCodeListener() {
        createPersonalAccountEmailCodePinEditText.onTextCompleteListener = object : OnTextCompleteListener {
            override fun onTextComplete(code: String): Boolean {
//                emailCodeViewModel.activateEmail(email, code)
                return false
            }
        }
    }

    private fun initChangeMailListener() =
        createPersonalAccountEmailCodeChangeMailTextView.setOnClickListener {
            requireActivity().onBackPressed()
        }

    companion object {
        private const val KEY_EMAIL = "email"

        fun newInstance(email: String) = CreatePersonalAccountEmailCodeFragment().withArgs(
            KEY_EMAIL to email
        )
    }
}
