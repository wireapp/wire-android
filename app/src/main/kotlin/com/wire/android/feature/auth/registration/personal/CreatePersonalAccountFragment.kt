package com.wire.android.feature.auth.registration.personal

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.core.extension.headingForAccessibility
import com.wire.android.core.extension.showKeyboard
import kotlinx.android.synthetic.main.fragment_create_personal_account.*
import org.koin.android.viewmodel.ext.android.viewModel

class CreatePersonalAccountFragment : Fragment(R.layout.fragment_create_personal_account) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCreateAnAccountTitle()
        initViewPager()
    }

    private fun initCreateAnAccountTitle() {
        createPersonalAccountTitleTextView.headingForAccessibility()
    }

    private fun initViewPager() {
        val titles = listOf(
            getString(R.string.authentication_tab_layout_title_email),
            getString(R.string.authentication_tab_layout_title_phone)
        )
        createPersonalAccountViewPager.adapter = CreatePersonalAccountViewPagerAdapter(childFragmentManager, titles)
        titles.forEachIndexed { index, title ->
            createPersonalAccountTabLayout.getTabAt(index)?.contentDescription =
                getString(R.string.create_an_account_tab_content_description, title, index + 1, titles.size)
        }
    }


    companion object {
        fun newInstance() = CreatePersonalAccountFragment()
    }
}
