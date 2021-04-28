package com.wire.android.feature.auth.client.ui
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.core.ui.navigation.Navigator
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.fragment_device_limit.deviceLimitLogoutButton
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class DeviceLimitFragment : Fragment(R.layout.fragment_device_limit) {

    private val viewModel by viewModel<DeviceLimitViewModel>()
    private val navigator by inject<Navigator>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideBackButton()
        disableBackButtonClick()
        observeCurrentSession()

        deviceLimitLogoutButton.setOnClickListener {
            viewModel.clearSession()
        }
    }

    private fun hideBackButton() {
        requireActivity().loginBackButton.visibility = View.GONE
    }

    private fun disableBackButtonClick() {
        requireActivity().onBackPressedDispatcher.addCallback(this) {}
    }

    private fun observeCurrentSession() {
        viewModel.isCurrentSessionDormantLiveData.observe(viewLifecycleOwner) { isSessionDormant ->
            if (isSessionDormant)
                navigator.welcome.openWelcomeScreen(requireContext())
        }
    }


    companion object {
        fun newInstance() = DeviceLimitFragment()
    }
}
