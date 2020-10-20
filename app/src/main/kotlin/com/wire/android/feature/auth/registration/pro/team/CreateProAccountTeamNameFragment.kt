package com.wire.android.feature.auth.registration.pro.team

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.wire.android.R
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.extension.headingForAccessibility
import com.wire.android.core.extension.showKeyboardWithFocusOn
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.ui.navigation.Navigator
import kotlinx.android.synthetic.main.fragment_create_pro_account_team_name.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class CreateProAccountTeamNameFragment : Fragment(R.layout.fragment_create_pro_account_team_name) {

    private val createProAccountTeamNameViewModel by viewModel<CreateProAccountTeamNameViewModel>()

    private val inputFocusViewModel: InputFocusViewModel by viewModel()

    private val navigator: Navigator by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTeamNameHeader()
        initTeamNameInput()
        initAboutButton()
        initConfirmationButton()
        observeTeamNameInput()
        requestKeyboardFocus()
    }

    private fun initTeamNameHeader() {
        createProAccountTeamNameTitleTextView.headingForAccessibility()
    }

    private fun initConfirmationButton() =
        with(createProAccountTeamNameInputConfirmationButton) {
            isEnabled = false
            setOnClickListener {
                navigator.createAccount.openProAccountTeamEmailScreen(requireContext())
            }
            createProAccountTeamNameViewModel.confirmationButtonEnabled.observe(viewLifecycleOwner) {
                isEnabled = it
            }
        }

    private fun initAboutButton() =
        createProAccountTeamNameAboutButton.setOnClickListener {
            navigator.createAccount.openProAccountAboutTeamScreen(requireContext())
        }

    private fun initTeamNameInput() {
        createProAccountTeamNameEditText.doOnTextChanged { text, _, _, _ ->
            createProAccountTeamNameViewModel.onTeamNameTextChanged(text.toStringOrEmpty())
        }
        createProAccountTeamNameEditText.doAfterTextChanged {
            createProAccountTeamNameViewModel.afterTeamNameChanged(it.toStringOrEmpty())
        }
    }

    private fun requestKeyboardFocus() {
        if (inputFocusViewModel.canFocusWithKeyboard()) showKeyboardWithFocusOn(
            createProAccountTeamNameEditText
        )
    }

    private fun observeTeamNameInput() =
        with(createProAccountTeamNameViewModel) {
            teamNameLiveData.observe(viewLifecycleOwner) {
                createProAccountTeamNameEditText.setText(it)
            }
        }

    companion object {
        fun newInstance() = CreateProAccountTeamNameFragment()
    }
}
