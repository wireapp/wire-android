package com.wire.android.feature.auth.client.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.core.ui.navigation.Navigator
import kotlinx.android.synthetic.main.fragment_device_limit.*
import org.koin.android.ext.android.inject

class DeviceLimitFragment : Fragment(R.layout.fragment_device_limit) {

    private val navigator by inject<Navigator>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLogoutButton()
    }

    private fun initLogoutButton() {
        deviceLimitLogoutButton.setOnClickListener {
            navigator.welcome.openWelcomeScreen(requireContext())
        }
    }

    companion object {
        fun newInstance() = DeviceLimitFragment()
    }
}
