package com.wire.android.feature.profile.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.core.extension.toast
import com.wire.android.shared.user.User
import kotlinx.android.synthetic.main.fragment_profile.*
import org.koin.android.viewmodel.ext.android.viewModel

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel by viewModel<ProfileViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpBackNavigation()

        fetchProfileInfo()

        displayUserInfo()
        displayTeamInfo()

        observeErrors()
    }

    private fun setUpBackNavigation() {
        profileToolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
    }

    private fun fetchProfileInfo() {
        viewModel.fetchProfileInfo()
    }

    private fun displayUserInfo() {
        viewModel.currentUserLiveData.observe(viewLifecycleOwner, ::updateUserInfo)
    }

    private fun updateUserInfo(user: User) {
        profileUserNameTextView.text = user.name
        profileHandleTextView.text = getString(R.string.profile_user_handle_text, user.username)
    }

    private fun displayTeamInfo() {
        viewModel.teamNameLiveData.observe(viewLifecycleOwner, ::updateTeamName)
    }

    private fun updateTeamName(teamName: String?) {
        if (teamName != null) {
            profileTeamInfoTextView.isVisible = true
            profileTeamInfoTextView.text = getString(R.string.profile_user_team_info_text, teamName)
        } else {
            profileTeamInfoTextView.isVisible = false
        }
    }

    private fun observeErrors() {
        viewModel.errorLiveData.observe(viewLifecycleOwner) {
            //TODO: display errors properly
            toast(it.message)
        }
    }
}
