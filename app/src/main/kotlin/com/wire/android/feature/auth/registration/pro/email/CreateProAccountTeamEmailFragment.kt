package com.wire.android.feature.auth.registration.pro.email

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.extension.headingForAccessibility
import com.wire.android.core.extension.showKeyboardWithFocusOn
import com.wire.android.core.extension.toStringOrEmpty
import kotlinx.android.synthetic.main.fragment_create_pro_account_team_email.*
import org.koin.android.viewmodel.ext.android.viewModel

class CreateProAccountTeamEmailFragment : Fragment(
    R.layout.fragment_create_pro_account_team_email
) {
    private val createProAccountTeamEmailViewModel: CreateProAccountTeamEmailViewModel by viewModel()

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTeamEmailHeader()
        initConfirmationButton()
        initTeamEmailInput()
        requestKeyboardFocus()
    }

    private fun initTeamEmailHeader() {
        createProAccountTeamEmailTitleTextView.headingForAccessibility()
    }

    private fun initConfirmationButton() =
        with(createProAccountTeamEmailInputConfirmationButton) {
            isEnabled = false
            setOnClickListener {
                //TODO Go to team email verification screen
            }
            createProAccountTeamEmailViewModel.confirmationButtonEnabled.observe(viewLifecycleOwner) {
                isEnabled = it
            }
        }

    private fun initTeamEmailInput() =
        with(createProAccountTeamEmailViewModel) {
            createProAccountTeamEmailEditText.doOnTextChanged { text, _, _, _ ->
                onTeamEmailTextChanged(text.toStringOrEmpty())
            }
            teamEmailLiveData.observe(viewLifecycleOwner) {
                createProAccountTeamEmailEditText.setText(it)
            }
        }

    private fun requestKeyboardFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(
            createProAccountTeamEmailEditText
        )
    }
}
