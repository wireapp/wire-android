package com.wire.android.feature.auth.login.email

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.wire.android.R
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.extension.toast
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import kotlinx.android.synthetic.main.fragment_login_with_email.*
import org.koin.android.viewmodel.ext.android.viewModel

class LoginWithEmailFragment : Fragment(R.layout.fragment_login_with_email) {

    private val viewModel: LoginWithEmailViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initInputListeners()
        initContinueButton()
        observeContinueStatus()
        observeLoginResult()
    }

    private fun initInputListeners() {
        loginWithEmailEditText.doAfterTextChanged {
            viewModel.validateEmail(it.toStringOrEmpty())
        }

        loginWithEmailPasswordEditText.doAfterTextChanged {
            viewModel.validatePassword(it.toStringOrEmpty())
        }
    }

    private fun initContinueButton() =
        loginWithEmailConfirmationButton.setOnClickListener {
            viewModel.login(
                email = loginWithEmailEditText.text.toStringOrEmpty(),
                password = loginWithEmailPasswordEditText.text.toStringOrEmpty()
            )
        }

    private fun observeContinueStatus() {
        viewModel.continueEnabledLiveData.observe(viewLifecycleOwner) {
            loginWithEmailConfirmationButton.isEnabled = it
        }
    }

    private fun observeLoginResult() {
        viewModel.loginResultLiveData.observe(viewLifecycleOwner) {
            it.onSuccess {
                //TODO navigate
                toast("Success!")
            }.onFailure {
                //TODO proper error dialog
                toast("Failure :(")
            }
        }
    }

    companion object {
        fun newInstance() = LoginWithEmailFragment()
    }
}
