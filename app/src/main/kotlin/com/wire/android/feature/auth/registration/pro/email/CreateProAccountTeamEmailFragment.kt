package com.wire.android.feature.auth.registration.pro.email

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.wire.android.R
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.extension.headingForAccessibility
import com.wire.android.core.extension.showKeyboardWithFocusOn
import kotlinx.android.synthetic.main.fragment_create_pro_account_team_email.*
import kotlinx.android.synthetic.main.fragment_create_pro_account_team_name.*
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
        observeTeamEmailInput()
        requestKeyboardFocus()
    }

    private fun initTeamEmailHeader() {
        createProAccountTeamEmailTitleTextView.headingForAccessibility()
    }

    private fun initConfirmationButton() =
        with(createProAccountTeamNameInputConfirmationButton) {
            isEnabled = false
            setOnClickListener {
                //TODO Go to team email verification screen
            }
            createProAccountTeamEmailViewModel.confirmationButtonEnabled.observe(viewLifecycleOwner) {
                isEnabled = it
            }
        }

    private fun observeTeamEmailInput() =
        with(createProAccountTeamEmailViewModel) {
            teamEmailLiveData.observe(viewLifecycleOwner) {
                createProAccountTeamNameEditText.setText(it)
            }
        }

    private fun requestKeyboardFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(
            createProAccountTeamNameEditText
        )
    }
}
