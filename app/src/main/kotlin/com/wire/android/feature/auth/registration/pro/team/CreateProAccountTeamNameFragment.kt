package com.wire.android.feature.auth.registration.pro.team

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.wire.android.R
import com.wire.android.core.accessibility.headingForAccessibility
import com.wire.android.core.extension.openUrl
import com.wire.android.core.extension.showKeyboard
import kotlinx.android.synthetic.main.fragment_create_pro_account_team_name.*
import org.koin.android.viewmodel.ext.android.viewModel

class CreateProAccountTeamNameFragment : Fragment(R.layout.fragment_create_pro_account_team_name) {

    private val createProAccountTeamNameViewModel by viewModel<CreateProAccountTeamNameViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTeamHeader()
        initTeamInput()
        initAboutButton()
        initConfirmationButton()
        observerUrlData()
        observeTeamInput()
    }

    private fun initTeamHeader() {
        createProAccountTeamNameTitleTextView.headingForAccessibility(true)
    }

    private fun initConfirmationButton() {
        with(createProAccountTeamNameInputConfirmationButton) {
            isEnabled = false
            setOnClickListener {
                //TODO Go to team email screen
            }
            createProAccountTeamNameViewModel.confirmationButtonEnabled.observe(viewLifecycleOwner) {
                isEnabled = it
            }
        }
    }

    private fun initAboutButton() =
        createProAccountTeamNameInputConfirmationButton.setOnClickListener {
            createProAccountTeamNameViewModel.onAboutButtonClicked()
        }

    private fun observerUrlData() {
        createProAccountTeamNameViewModel.urlLiveData.observe(viewLifecycleOwner) {
            openUrl(it)
        }
    }

    private fun initTeamInput() {
        createProAccountTeamNameEditText.doOnTextChanged { text, _, _, _ ->
            createProAccountTeamNameViewModel.onTeamNameTextChanged(text.toString())
        }
        createProAccountTeamNameEditText.doAfterTextChanged {
            createProAccountTeamNameViewModel.afterTeamNameChanged(it.toString())
        }
    }

    private fun observeTeamInput() {
        with(createProAccountTeamNameViewModel) {
            textInputFocusedLiveData.observe(viewLifecycleOwner) {
                showKeyboard()
                createProAccountTeamNameEditText.requestFocus()
            }
            teamNameLiveData.observe(viewLifecycleOwner) {
                createProAccountTeamNameEditText.setText(it)
            }
        }
    }

    companion object {
        fun newInstance() = CreateProAccountTeamNameFragment()
    }
}
